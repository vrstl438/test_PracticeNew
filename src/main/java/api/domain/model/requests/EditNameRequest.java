package api.domain.model.requests;

import api.domain.model.RequestModel;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EditNameRequest implements RequestModel {
    private String name;
}
