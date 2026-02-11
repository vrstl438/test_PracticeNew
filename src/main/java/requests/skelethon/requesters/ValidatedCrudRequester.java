package requests.skelethon.requesters;

import domain.model.RequestModel;
import domain.model.ResponseModel;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

public class ValidatedCrudRequester<T extends ResponseModel> extends HttpRequest implements CrudEndpointInterface {

    private final CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public T post() {
        return (T) crudRequester.post().extract().as(endpoint.getResponseModel());
    }

    @Override
    public T post(RequestModel model) {
        return (T) crudRequester.post(model).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T put() {
        return (T) crudRequester.put().extract().as(endpoint.getResponseModel());
    }

    @Override
    public T put(RequestModel model) {
        return (T) crudRequester.put(model).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T get() {
        return (T) crudRequester.get().extract().as(endpoint.getResponseModel());
    }

    @Override
    public T get(long id) {
        return (T) crudRequester.get(id).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T update(long id, RequestModel model) {
        return (T) crudRequester.update(id, model).extract().as(endpoint.getResponseModel());
    }

    @Override
    public T delete(long id) {
        return (T) crudRequester.delete(id).extract().as(endpoint.getResponseModel());
    }
}
