package audit;
import org.junit.jupiter.api.*;
import java.util.*;
public final class RegressionSuiteTest {
@TestFactory List<DynamicTest> existingSuites(){return List.of(DynamicTest.dynamicTest("LootRecoveryTest", () -> site.cadera.arunika.dungeon.battle.LootRecoveryTest.main(new String[0])),
DynamicTest.dynamicTest("BattleTest", () -> site.cadera.arunika.dungeon.battle.BattleTest.main(new String[0])),
DynamicTest.dynamicTest("RpgTest", () -> site.cadera.arunika.dungeon.battle.RpgTest.main(new String[0])),
DynamicTest.dynamicTest("TeleportSafetyTest", () -> site.cadera.arunika.dungeon.battle.TeleportSafetyTest.main(new String[0])),
DynamicTest.dynamicTest("LootTest", () -> site.cadera.arunika.dungeon.battle.LootTest.main(new String[0])),
DynamicTest.dynamicTest("GatewayTest", () -> site.cadera.arunika.dungeon.battle.GatewayTest.main(new String[0])),
DynamicTest.dynamicTest("DatabaseMigrationTest", () -> site.cadera.arunika.dungeon.core.DatabaseMigrationTest.main(new String[0])),
DynamicTest.dynamicTest("CoreTest", () -> site.cadera.arunika.dungeon.core.CoreTest.main(new String[0])),
DynamicTest.dynamicTest("PerformanceGuardTest", () -> site.cadera.arunika.dungeon.core.PerformanceGuardTest.main(new String[0])),
DynamicTest.dynamicTest("ShutdownTest", () -> site.cadera.arunika.dungeon.core.ShutdownTest.main(new String[0])),
DynamicTest.dynamicTest("SharedTest", () -> site.cadera.arunika.dungeon.shared.SharedTest.main(new String[0])),
DynamicTest.dynamicTest("RecoveryJournalTest", () -> site.cadera.arunika.dungeon.config.RecoveryJournalTest.main(new String[0])),
DynamicTest.dynamicTest("AdminConfigTest", () -> site.cadera.arunika.dungeon.config.AdminConfigTest.main(new String[]{"site.cadera.arunika.dungeon.config.SnakeYamlCodec"})),
DynamicTest.dynamicTest("ConfigTest", () -> site.cadera.arunika.dungeon.config.ConfigTest.main(new String[]{"site.cadera.arunika.dungeon.config.SnakeYamlCodec"})),
DynamicTest.dynamicTest("PrivateTest", () -> site.cadera.arunika.dungeon.privategame.PrivateTest.main(new String[0])),
DynamicTest.dynamicTest("SimpleWaystonePartyTest", () -> site.cadera.arunika.dungeon.privategame.SimpleWaystonePartyTest.main(new String[0])),
DynamicTest.dynamicTest("GatewayMenuStyleTest", () -> site.cadera.arunika.dungeon.paper.GatewayMenuStyleTest.main(new String[0])),
DynamicTest.dynamicTest("GatewayPhase6Test", () -> site.cadera.arunika.dungeon.paper.GatewayPhase6Test.main(new String[0])),
DynamicTest.dynamicTest("GatewayAimTest", () -> site.cadera.arunika.dungeon.paper.GatewayAimTest.main(new String[0])),
DynamicTest.dynamicTest("AccountingGuardTest", () -> site.cadera.arunika.dungeon.exchange.AccountingGuardTest.main(new String[0])),
DynamicTest.dynamicTest("ExchangeTest", () -> site.cadera.arunika.dungeon.exchange.ExchangeTest.main(new String[]{"jdbc"})),
DynamicTest.dynamicTest("ExchangeStressTest", () -> site.cadera.arunika.dungeon.exchange.ExchangeStressTest.main(new String[0])));}
}
