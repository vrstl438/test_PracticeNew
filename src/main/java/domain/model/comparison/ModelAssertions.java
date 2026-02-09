package domain.model.comparison;

import domain.model.Role;
import domain.model.requests.DepositRequest;
import domain.model.requests.EditNameRequest;
import domain.model.requests.TransferRequest;
import domain.model.response.AccountResponse.Transaction;
import domain.model.response.DepositResponse;
import domain.model.response.EditUserResponse;
import domain.model.response.TransferResponse;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.data.Offset;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelAssertions extends AbstractAssert<ModelAssertions, Object> {

    private static final String CONFIG_FILE = "model-comparison.properties";
    private static final ModelComparisonConfigLoader configLoader = new ModelComparisonConfigLoader(CONFIG_FILE);

    private final Object request;
    private final Object response;

    private ModelAssertions(Object request, Object response) {
        super(request, ModelAssertions.class);
        this.request = request;
        this.response = response;
    }

    public static ModelAssertions assertThatModels(Object request, Object response) {
        return new ModelAssertions(request, response);
    }

    public ModelAssertions match() {
        ModelComparisonConfigLoader.ComparisonRule rule = configLoader.getRuleFor(request.getClass());

        if (rule == null) {
            failWithMessage("No comparison rule found for class %s in %s",
                    request.getClass().getSimpleName(), CONFIG_FILE);
            return this;
        }

        ModelComparator.ComparisonResult result = ModelComparator.compareFields(
                request,
                response,
                rule.getFieldMappings()
        );

        if (!result.isSuccess()) {
            failWithMessage("Model comparison failed for %s:\n%s",
                    request.getClass().getSimpleName(), result);
        }

        return this;
    }

    public ModelAssertions responseIsNotNull() {
        if (response == null) {
            failWithMessage("Expected response to be not null");
        }
        return this;
    }

    public static void assertDepositCreated(DepositResponse response, DepositRequest request) {
        assertThat(response.getId()).isEqualTo(request.getId());
        assertThat(response.getAccountNumber()).isNotNull().hasSizeGreaterThanOrEqualTo(3);
        assertThat(response.getBalance()).isEqualTo(request.getBalance());

        assertThat(response.getTransactions()).isNotNull().isNotEmpty();

        Transaction transaction = response.getTransactions().get(0);
        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getAmount()).isEqualTo(request.getBalance());
        assertThat(transaction.getType()).isEqualTo("DEPOSIT");
        assertThat(transaction.getTimestamp()).isNotNull();
        assertThat(transaction.getRelatedAccountId()).isEqualTo(response.getId());
    }

    public static void assertTransferCreated(TransferResponse response, TransferRequest request) {
        assertThat(response.getMessage()).isEqualTo("Transfer successful");
        assertThat(response.getAmount()).isNotNull().isPositive();
        assertThat(response.getAmount()).isEqualTo(request.getAmount());
        assertThat(response.getReceiverAccountId()).isEqualTo(request.getReceiverAccountId());
        assertThat(response.getReceiverAccountId()).isNotNull().isPositive();
        assertThat(response.getSenderAccountId()).isEqualTo(request.getSenderAccountId());
        assertThat(response.getSenderAccountId()).isNotNull().isPositive();
    }

    public static void assertEditUserResponse(EditUserResponse response, EditNameRequest request) {
        assertThat(response.getMessage()).isEqualTo("Profile updated successfully");
        assertThat(response.getCustomer()).isNotNull();
        assertThat(response.getCustomer().getId()).isNotNull().isPositive();
        assertThat(response.getCustomer().getUsername()).isNotNull().isNotEmpty();
        assertThat(response.getCustomer().getName()).isEqualTo(request.getName());
        assertThat(response.getCustomer().getRole()).isNotNull().isEqualTo(Role.USER);
        assertThat(response.getCustomer().getAccounts()).isNotNull();
    }

    public static void assertTransactions(List<Transaction> transactions, int expectedSize, Double expectedTotalAmount) {
        assertThat(transactions).hasSize(expectedSize);

        Double totalAmount = transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();
        assertThat(totalAmount).isCloseTo(expectedTotalAmount, Offset.offset(0.01));
    }

    public static void assertPlainErrorMessage(Response response, String expectedMessage) {
        assertThat(response.getContentType()).contains(ContentType.TEXT.toString());
        assertThat(response.asString().trim()).isEqualTo(expectedMessage);
    }
}
