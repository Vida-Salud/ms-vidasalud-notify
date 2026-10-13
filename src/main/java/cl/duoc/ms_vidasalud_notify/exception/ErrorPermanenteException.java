package cl.duoc.ms_vidasalud_notify.exception;

public class ErrorPermanenteException extends RuntimeException {

    public ErrorPermanenteException(String message) {
        super(message);
    }

    public ErrorPermanenteException(String message, Throwable cause) {
        super(message, cause);
    }
}
