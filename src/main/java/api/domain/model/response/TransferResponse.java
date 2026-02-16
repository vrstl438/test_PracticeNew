package api.domain.model.response;

import api.domain.model.ResponseModel;
import lombok.Getter;

@Getter
public class TransferResponse implements ResponseModel {
    private String message;
    private Double amount;
    private Long receiverAccountId;
    private Long senderAccountId;
}
