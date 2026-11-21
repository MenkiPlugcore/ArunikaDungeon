package site.cadera.arunika.dungeon.core;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Isolate cleanup failures so one incompatible optional dependency cannot skip later teardown. */
public final class ShutdownSteps {
    private final BiConsumer<String, Throwable> report;
    public ShutdownSteps(BiConsumer<String, Throwable> report) {
        this.report = Objects.requireNonNull(report);
    }
    public void run(String name, Runnable step) {
        try { step.run(); }
        catch (RuntimeException | LinkageError failure) { report.accept(name, failure); }
    }
}
