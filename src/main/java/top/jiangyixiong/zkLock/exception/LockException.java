package top.jiangyixiong.zkLock.exception;

/**
 * @author Yixiong
 *
 * Exception for lock operations
 *
 */
public class LockException extends RuntimeException {
    public LockException(String message){
        super(message);
    }
}
