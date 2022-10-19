package steps;

import io.qameta.allure.Step;

public class AllureSteps {

    @Step("{0}: {1}")
    public static void logToAllure(String message, Object parameter) {
    }

    //TODO implement logResponse / logRequest toAllure steps
}
