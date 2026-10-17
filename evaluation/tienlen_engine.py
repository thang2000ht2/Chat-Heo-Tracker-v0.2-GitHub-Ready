"""Experimental, explicit-reference Tiến Lên rule engine for RESEARCH/self-play.
NOT claimed to match Pig Hunters; see RULE_COVERAGE.md. Standard library only.
Card = integer 4*rank+suit; ranks 3..A,2; suits S,C,D,H.
"""
from __future__ import annotations
from collections import Counter
from dataclasses import dataclass
from functools import lru_cache
from itertools import combinations, product
from typing import Optional

RANKS = ('3','4','5','6','7','8','9','10','J','Q','K','A','2')
SUITS = ('S','C','D','H')
DECK = tuple(range(52))


def card(c):
    return RANKS[c//4]+SUITS[c%4]


def parse(c):
    return 4*RANKS.index(c[:-1])+SUITS.index(c[-1])

@dataclass(frozen=True)
class Rules:
    first_must_contain_3s: bool = True
    three_pairs_chop_single_two: bool = True
    four_chop_single_two: bool = True
    four_chop_pair_two: bool = True
    four_pairs_chop_pair_two: bool = True
    four_pairs_chop_single_two: bool = True
    four_pairs_chop_four: bool = True
    special_chop_chain: bool = True
    allow_out_of_turn_chop: bool = False  # intentionally NOT IMPLEMENTED
    instant_win: bool = False             # unsupported -> refuse this mode

@dataclass(frozen=True)
class Move:
    cards: tuple[int,...]
    kind: str
    high: int
    top_suit: int

    @property
    def count(self): return len(self.cards)


def classify(cards) -> Optional[Move]:
    cs=tuple(sorted(cards))
    if not cs or len(set(cs))!=len(cs) or any(not isinstance(c,int) or c<0 or c>=52 for c in cs):return None
    rs=[c//4 for c in cs]; d=Counter(rs); n=len(cs)
    high=max(rs); suit=max(c%4 for c in cs if c//4==high)
    if n==1:return Move(cs,'single',high,suit)
    if len(d)==1 and n in (2,3,4):return Move(cs,{2:'pair',3:'triple',4:'four'}[n],high,suit)
    if len(d)==n and n>=3 and high<12 and high-min(rs)+1==n:
        return Move(cs,'straight',high,suit)
    if n in (6,8) and len(d)==n//2 and all(x==2 for x in d.values()) and high<12 and high-min(rs)+1==n//2:
        return Move(cs,'pairs3' if n==6 else 'pairs4',high,suit)
    return None


def beats(move:Move,lead:Move,rules:Rules=Rules()):
    if move.kind==lead.kind and move.count==lead.count:
        return (move.high,move.top_suit)>(lead.high,lead.top_suit)
    if lead.kind=='single' and lead.high==12:
        return (move.kind=='pairs3' and rules.three_pairs_chop_single_two or
                move.kind=='four' and rules.four_chop_single_two or
                move.kind=='pairs4' and rules.four_pairs_chop_single_two)
    if lead.kind=='pair' and lead.high==12:
        return (move.kind=='four' and rules.four_chop_pair_two or
                move.kind=='pairs4' and rules.four_pairs_chop_pair_two)
    if rules.special_chop_chain:
        return (lead.kind=='pairs3' and move.kind in ('four','pairs4') or
                lead.kind=='four' and move.kind=='pairs4' and rules.four_pairs_chop_four)
    return False


@lru_cache(maxsize=1024)
def all_moves(hand:tuple[int,...]):
    """Exact legal groups from the currently held cards, generated once per hand."""
    by_rank={r:tuple(c for c in hand if c//4==r) for r in range(13)}
    out=[]
    for r,cc in by_rank.items():
        for n in range(1,min(4,len(cc))+1):
            for combo in combinations(cc,n):
                m=classify(combo)
                if m:out.append(m)
    # Straight: one card from each consecutive rank (2 is excluded).
    for start in range(12):
        run=[]
        for r in range(start,12):
            if not by_rank[r]:break
            run.append(by_rank[r])
            if len(run)>=3:
                for combo in product(*run):
                    out.append(Move(tuple(sorted(combo)),'straight',r,max(c%4 for c in combo if c//4==r)))
    # Consecutive pairs; choose any 2 of each rank.
    for length in (3,4):
        for start in range(13-length):
            choices=[tuple(combinations(by_rank[r],2)) for r in range(start,start+length)]
            if all(choices):
                for cp in product(*choices):
                    cs=tuple(sorted(c for pair in cp for c in pair))
                    out.append(Move(cs,'pairs'+str(length),start+length-1,max(c%4 for c in cp[-1])))
    return tuple(out)


def legal_moves(hand,lead,rules=Rules(),first=False):
    if rules.allow_out_of_turn_chop or rules.instant_win:
        raise NotImplementedError('Out-of-turn chặt and tới trắng need verified game rules before deployment')
    moves=all_moves(tuple(sorted(hand)))
    if first and rules.first_must_contain_3s:
        moves=tuple(m for m in moves if 0 in m.cards)
    if lead is not None:moves=tuple(m for m in moves if beats(m,lead,rules))
    return moves


def first_turn(hand):
    return 0 in hand


def deal(rng):
    shuffled=list(DECK);rng.shuffle(shuffled)
    return [tuple(sorted(shuffled[i*13:(i+1)*13])) for i in range(4)]


def compact(move):
    return ' '.join(card(c) for c in move.cards) if move else 'PASS'
