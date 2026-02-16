package api.domain.builders;

import api.domain.model.requests.DepositRequest;

public class CreateDepositRequestBuilder {
    private Long id = null;
    private Double balance = 1 + Math.random() * 4999;

    public CreateDepositRequestBuilder withId(Long id){
        this.id = id;
        return this;
    }

    public CreateDepositRequestBuilder withBalance(Double balance){
        this.balance = balance;
        return this;
    }

    public DepositRequest depositBuild(){
        return new DepositRequest(id, balance);
    }
}