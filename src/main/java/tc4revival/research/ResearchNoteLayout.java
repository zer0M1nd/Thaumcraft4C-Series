package tc4revival.research;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** An exact initial board. Size is the hexagon side length; omitted cells are empty. */
public record ResearchNoteLayout(int size, Map<ResearchHex, Cell> cells) {
    public enum State { EMPTY, BLOCKED, INITIAL, NEGATIVE }

    public record Cell(State state, String aspect) {
        public Cell {
            Objects.requireNonNull(state);
            boolean occupied = state == State.INITIAL || state == State.NEGATIVE;
            if (occupied ? aspect == null || aspect.isBlank() : aspect != null)
                throw new IllegalArgumentException("Only initial/negative layout cells require an aspect");
        }
    }

    public ResearchNoteLayout {
        if (size < 1 || size > 5) throw new IllegalArgumentException("Note layout size must be 1..5");
        var captured = new LinkedHashMap<ResearchHex, Cell>();
        cells.forEach((hex, cell) -> {
            // Check each coordinate before radius() to avoid integer overflow for malformed input.
            if (hex.q() < 1 - size || hex.q() >= size || hex.r() < 1 - size || hex.r() >= size
                    || hex.radius() >= size) throw new IllegalArgumentException("Layout cell outside board: " + hex);
            captured.put(hex, Objects.requireNonNull(cell));
        });
        cells = Collections.unmodifiableMap(captured);
    }

    /** The existing note format represents obstacles by absent coordinates. */
    public Map<ResearchHex, ResearchNote.Cell> initialGrid() {
        var grid = new LinkedHashMap<ResearchHex, ResearchNote.Cell>();
        grid.put(new ResearchHex(0, 0), ResearchNote.Cell.blank());
        for (int radius = 1; radius < size; radius++)
            for (var hex : ResearchHex.ring(radius)) grid.put(hex, ResearchNote.Cell.blank());
        cells.forEach((hex, cell) -> {
            switch (cell.state()) {
                case EMPTY -> grid.put(hex, ResearchNote.Cell.blank());
                case BLOCKED -> grid.remove(hex);
                case INITIAL -> grid.put(hex, new ResearchNote.Cell(1, cell.aspect()));
                case NEGATIVE -> grid.put(hex, new ResearchNote.Cell(3, cell.aspect()));
            }
        });
        return Collections.unmodifiableMap(grid);
    }

    public static ResearchNoteLayout decode(JsonElement source) {
        if (source == null || !source.isJsonObject())
            throw new IllegalArgumentException("noteLayout must be an object");
        var value = source.getAsJsonObject();
        fields(value, Set.of("size", "cells"));
        int size = integer(value, "size");
        if (!value.has("cells") || !value.get("cells").isJsonArray())
            throw new IllegalArgumentException("noteLayout.cells must be an array");
        var cells = new LinkedHashMap<ResearchHex, Cell>();
        for (var element : value.getAsJsonArray("cells")) {
            if (!element.isJsonObject()) throw new IllegalArgumentException("Layout cell must be an object");
            var row = element.getAsJsonObject();
            fields(row, Set.of("q", "r", "state", "aspect"));
            var hex = new ResearchHex(integer(row, "q"), integer(row, "r"));
            var state = switch (string(row, "state")) {
                case "empty" -> State.EMPTY;
                case "blocked" -> State.BLOCKED;
                case "initial" -> State.INITIAL;
                case "negative" -> State.NEGATIVE;
                default -> throw new IllegalArgumentException("Unknown note layout state: " + row.get("state"));
            };
            var cell = new Cell(state, row.has("aspect") ? string(row, "aspect") : null);
            if (cells.putIfAbsent(hex, cell) != null)
                throw new IllegalArgumentException("Duplicate note layout coordinate: " + hex);
        }
        return new ResearchNoteLayout(size, cells);
    }

    private static void fields(JsonObject value, Set<String> allowed) {
        for (String field : value.keySet()) if (!allowed.contains(field))
            throw new IllegalArgumentException("Unknown note layout field: " + field);
    }

    private static int integer(JsonObject value, String field) {
        var number = value.get(field);
        if (number == null || !number.isJsonPrimitive() || !number.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Note layout " + field + " must be an integer");
        try { return number.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException("Note layout " + field + " must be an integer", failure);
        }
    }

    private static String string(JsonObject value, String field) {
        var text = value.get(field);
        if (text == null || !text.isJsonPrimitive() || !text.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Note layout " + field + " must be a string");
        return text.getAsString();
    }
}
