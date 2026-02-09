package domain.model.response;

import domain.model.ResponseModel;
import lombok.Getter;

@Getter
public class TransferResponse implements ResponseModel {
    private String message;
    private Double amount;
    private Long receiverAccountId;
    private Long senderAccountId;
}
