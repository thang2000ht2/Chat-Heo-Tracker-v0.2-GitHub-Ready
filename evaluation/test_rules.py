import unittest,random
from tienlen_engine import *
from selfplay import run_game
class RulesTest(unittest.TestCase):
    def C(self,*cards):return [parse(c) for c in cards]
    def m(self,*cards):return classify(self.C(*cards))
    def test_moves(self):
        self.assertEqual(self.m('3S').kind,'single')
        self.assertEqual(self.m('7S','7H').kind,'pair')
        self.assertEqual(self.m('5S','5C','5D').kind,'triple')
        self.assertEqual(self.m('8S','8C','8D','8H').kind,'four')
        self.assertEqual(self.m('4S','5H','6D').kind,'straight')
        self.assertIsNone(self.m('QS','KS','2S'))
        self.assertEqual(self.m('5S','5C','6S','6C','7D','7H').kind,'pairs3')
        self.assertEqual(self.m('5S','5C','6S','6C','7D','7H','8S','8C').kind,'pairs4')
    def test_special_chops(self):
        pig=self.m('2S');pairtwo=self.m('2S','2H')
        run3=self.m('5S','5C','6S','6C','7D','7H')
        quad=self.m('8S','8C','8D','8H')
        run4=self.m('5S','5C','6S','6C','7D','7H','8S','8C')
        self.assertTrue(beats(run3,pig))
        self.assertTrue(beats(quad,pig))
        self.assertTrue(beats(quad,pairtwo))
        self.assertTrue(beats(run4,pairtwo))
        self.assertTrue(beats(quad,run3))
        self.assertTrue(beats(run4,quad))
        self.assertFalse(beats(run3,pairtwo))
        self.assertFalse(beats(self.m('JS'),self.m('QH')))
        self.assertFalse(beats(quad,pairtwo,Rules(four_chop_pair_two=False)))
    def test_first_3s(self):
        cards=self.C('3S','4S','4H')
        self.assertTrue(all(parse('3S') in m.cards for m in legal_moves(cards,None,first=True)))
    def test_trick_runs(self):
        for seed in range(30):
            h=deal(random.Random(seed))
            ranks,events=run_game(h,['lowest','tempo','max_shed','save_twos'],seed=seed)
            self.assertEqual(sorted(ranks),[0,1,2,3])
            self.assertEqual(sum(e['type']=='move' for e in events)>0,True)
            observed=[c for e in events if e['type']=='move' for c in e['cards']]
            self.assertEqual(len(observed),len(set(observed)))
            self.assertGreaterEqual(len(observed),39)  # final loser may keep cards
    def test_unsupported_rule_modes_refuse(self):
        with self.assertRaises(NotImplementedError):legal_moves(self.C('3S'),None,Rules(instant_win=True))
        with self.assertRaises(NotImplementedError):legal_moves(self.C('3S'),None,Rules(allow_out_of_turn_chop=True))
if __name__=='__main__':unittest.main()
