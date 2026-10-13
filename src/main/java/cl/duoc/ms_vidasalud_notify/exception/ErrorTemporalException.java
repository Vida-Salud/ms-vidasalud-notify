package cl.duoc.ms_vidasalud_notify.exception;

public class ErrorTemporalException extends RuntimeException {

    public ErrorTemporalException(String message) {
        super(message);
    }

    public ErrorTemporalException(String message, Throwable cause) {
        super(message, cause);
    }
}
