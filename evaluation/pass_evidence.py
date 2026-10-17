"""Experimental posterior weighting of a *confirmed* pass, never hard elimination.
Weights MUST be calibrated from genuine labelled opportunities before use.
Not called by the live Android suggestion engine.
"""
from dataclasses import dataclass
from typing import Optional
from tienlen_engine import Move,Rules,legal_moves

@dataclass(frozen=True)
class CalibratedPassModel:
    p_pass_when_able:float
    p_pass_when_unable:float
    sample_count:int
    def is_ready(self):
        return (self.sample_count>=200 and
                0<self.p_pass_when_able<1 and
                0<self.p_pass_when_unable<1 and
                self.p_pass_when_able<=self.p_pass_when_unable)


def pass_likelihood(hand:tuple[int,...],lead:Move,model:Optional[CalibratedPassModel],rules=Rules()):
    """Only for visually CONFIRMED pass events with a verified live lead.
    Missing observations or an untrained model produce None (ABSTAIN).
    Even if able, result MUST be nonzero because passing is strategic.
    """
    if lead is None or model is None or not model.is_ready():return None
    has_reply=bool(legal_moves(hand,lead,rules))
    return model.p_pass_when_able if has_reply else model.p_pass_when_unable
