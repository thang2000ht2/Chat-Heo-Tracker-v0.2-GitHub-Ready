import unittest
from pass_evidence import CalibratedPassModel,pass_likelihood
from tienlen_engine import parse,classify
class TestPassEvidence(unittest.TestCase):
    def test_pass_is_not_impossibility(self):
        lead=classify((parse('9S'),))
        hand=(parse('10S'),parse('3S'))
        self.assertIsNone(pass_likelihood(hand,lead,None))
        self.assertIsNone(pass_likelihood(hand,lead,CalibratedPassModel(.25,.98,10)))
        m=CalibratedPassModel(.25,.98,300)
        self.assertAlmostEqual(pass_likelihood(hand,lead,m),.25)
        self.assertAlmostEqual(pass_likelihood((parse('3S'),parse('4C')),lead,m),.98)
if __name__=='__main__':unittest.main()
