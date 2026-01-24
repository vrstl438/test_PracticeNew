package domain.builders;

import domain.model.requests.TransferRequest;

public class CreateTransferRequestBuilder {
    private Long senderAccountId = null;
    private Long receiverAccountId = null;
    private Double amount = 1 + Math.random() * 9999;

    public CreateTransferRequestBuilder withSenderAccountId(Long senderAccountId){
        this.senderAccountId = senderAccountId;
        return this;
    }

    public CreateTransferRequestBuilder withReceiverAccountId(Long receiverAccountId){
        this.receiverAccountId = receiverAccountId;
        return this;
    }

    public CreateTransferRequestBuilder withAmount(Double amount){
        this.amount = amount;
        return this;
    }

    public TransferRequest transferBuild () {
        return new TransferRequest(senderAccountId, receiverAccountId, amount);
    }
}