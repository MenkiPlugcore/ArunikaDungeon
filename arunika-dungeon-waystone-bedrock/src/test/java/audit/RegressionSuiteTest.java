package audit;
import org.junit.jupiter.api.*;
import java.util.*;
public final class RegressionSuiteTest {
@TestFactory List<DynamicTest> existingSuites(){return List.of(DynamicTest.dynamicTest("ModelCatalogTest", () -> dev.arunika.dungeonwaystone.bedrock.ModelCatalogTest.main(new String[0])));}
}
