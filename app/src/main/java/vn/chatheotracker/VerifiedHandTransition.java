package vn.chatheotracker;

import java.util.*;

/**
 * A verified local hand may only shrink when the identities of ALL removed
 * cards have independently appeared in public observations. A complete-looking
 * one-card fan is not proof that 3 other cards were played: animations and
 * overlays can produce a stable but *incorrect* partial fan.
 *
 * This class deliberately does not infer WHO played a public card. Its job is
 * only to prevent replacement of a verified hand with an uncorroborated one.
 */
final class VerifiedHandTransition {
    enum Status { SAME, CONFIRMED_SHRINK, WAIT_PUBLIC_CARDS, REJECT_CHANGED_CARDS, REJECT_PUBLIC_OVERLAP }

    static final class Result {
        final Status status;
        final List<String> removed;
        final List<String> missingPublicEvidence;
        Result(Status status,Collection<String> removed,Collection<String> missing){
            this.status=status;
            this.removed=Collections.unmodifiableList(new ArrayList<>(removed));
            this.missingPublicEvidence=Collections.unmodifiableList(new ArrayList<>(missing));
        }
        boolean confirmed(){return status==Status.CONFIRMED_SHRINK;}
    }

    static List<String> publicHandOverlap(Collection<String> hand,Collection<String> observed){
        if(hand==null||observed==null)return Collections.emptyList();
        LinkedHashSet<String> colliding=new LinkedHashSet<>(hand);
        colliding.retainAll(observed);
        return new ArrayList<>(colliding);
    }

    static Result assess(Collection<String> before,Collection<String> after,
                         Collection<String> observedPublic){
        if(before==null||after==null||observedPublic==null)
            return new Result(Status.REJECT_CHANGED_CARDS,Collections.emptyList(),Collections.emptyList());
        LinkedHashSet<String> previous=new LinkedHashSet<>(before);
        LinkedHashSet<String> candidate=new LinkedHashSet<>(after);
        LinkedHashSet<String> publicCards=new LinkedHashSet<>(observedPublic);
        if(previous.size()!=before.size() || candidate.size()!=after.size() ||
                previous.isEmpty() || candidate.isEmpty())
            return new Result(Status.REJECT_CHANGED_CARDS,Collections.emptyList(),Collections.emptyList());
        if(previous.equals(candidate))
            return new Result(Status.SAME,Collections.emptyList(),Collections.emptyList());
        if(candidate.size()>=previous.size() || !previous.containsAll(candidate))
            return new Result(Status.REJECT_CHANGED_CARDS,Collections.emptyList(),Collections.emptyList());
        LinkedHashSet<String> removed=new LinkedHashSet<>(previous);
        removed.removeAll(candidate);
        // A card visible on the table CANNOT also be in our current hand.
        if(!Collections.disjoint(candidate,publicCards))
            return new Result(Status.REJECT_PUBLIC_OVERLAP,removed,Collections.emptyList());
        LinkedHashSet<String> missing=new LinkedHashSet<>(removed);
        missing.removeAll(publicCards);
        if(!missing.isEmpty())
            return new Result(Status.WAIT_PUBLIC_CARDS,removed,missing);
        return new Result(Status.CONFIRMED_SHRINK,removed,Collections.emptyList());
    }
    private VerifiedHandTransition(){}
}
