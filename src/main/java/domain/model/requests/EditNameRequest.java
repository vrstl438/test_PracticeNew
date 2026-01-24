package domain.model.requests;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EditNameRequest {
    private String name;
}
