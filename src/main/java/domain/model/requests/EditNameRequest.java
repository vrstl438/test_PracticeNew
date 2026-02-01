package domain.model.requests;

import domain.model.RequestModel;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EditNameRequest implements RequestModel {
    private String name;
}
