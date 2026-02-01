package requests.skelethon.requesters;

import domain.model.RequestModel;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;

public class CrudRequester extends HttpRequest implements CrudEndpointInterface {

    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse post() {
        return given()
                .spec(requestSpecification)
                .post(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse post(RequestModel model) {
        return requestWithBody(model)
                .post(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put() {
        return given()
                .spec(requestSpecification)
                .put(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(RequestModel model) {
        return requestWithBody(model)
                .put(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get(long id) {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getUrl() + "/" + id)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    public <T> List<T> getList(Object... params) {
        T[] array = (T[]) given()
                .spec(requestSpecification)
                .get(endpoint.formatUrl(params))
                .then()
                .assertThat()
                .spec(responseSpecification)
                .extract().as(endpoint.getResponseModel());
        return Arrays.asList(array);
    }

    @Override
    public ValidatableResponse update(long id, RequestModel model) {
        return requestWithBody(model)
                .put(endpoint.getUrl() + "/" + id)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse delete(long id) {
        return given()
                .spec(requestSpecification)
                .delete(endpoint.getUrl() + "/" + id)
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    private io.restassured.specification.RequestSpecification requestWithBody(RequestModel model) {
        return given()
                .spec(requestSpecification)
                .body(model);
    }
}
