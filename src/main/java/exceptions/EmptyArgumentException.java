package exceptions;

public class EmptyArgumentException extends IllegalArgumentException {

    private static final long serialVersionUID = -4862926644813433707L;

    public EmptyArgumentException() {
        super();
    }

    public EmptyArgumentException(String message) {
        super(message);
    }

    public EmptyArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyArgumentException(Throwable cause) {
        super(cause);
    }

}