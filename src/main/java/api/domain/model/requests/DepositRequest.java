package api.domain.model.requests;

import api.domain.model.RequestModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest implements RequestModel {
    private Long id;
    private Double balance;
}
