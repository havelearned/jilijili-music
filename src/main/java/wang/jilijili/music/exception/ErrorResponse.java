package wang.jilijili.music.exception;

import lombok.Data;

/**
 * @Auther: Amani
 * @Date: 2023/1/28 11:04
 * @Description:
 */

@Data
public class ErrorResponse {
    private Integer code;
    private String message;
    private Object trace;

}