import org.omg.CORBA.ARG_IN;

import java.security.InvalidParameterException;

public enum Segment {
    CONSTANT,
    ARGUMENT,
    LOCAL,
    STATIC,
    THIS,
    THAT,
    POINTER,
    TEMP,;

    public static Segment segmentOf(Kind kind) {
        switch (kind) {
            case VAR:
                return LOCAL;
            case FIELD:
                return THIS;
            case ARG:
                return ARGUMENT;
            case STATIC:
                return STATIC;
        }
        throw new InvalidParameterException("Kind " + kind.name() + " must be one of VAR|FIELD|ARG|STATIC");
    }
}
