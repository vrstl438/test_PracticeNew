package requests.skelethon.interfaces;

import domain.model.RequestModel;

public interface CrudEndpointInterface {
    Object post();
    Object post(RequestModel model);
    Object put();
    Object put(RequestModel model);
    Object get();
    Object get(long id);
    Object get(String path);
    Object update(long id, RequestModel model);
    Object delete(long id);
}
