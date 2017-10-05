package com.github.tomakehurst.wiremock.stubbing;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;

public class Scenarios {

    private final ConcurrentHashMap<String, Scenario> scenarioMap = new ConcurrentHashMap<>();

    public void onStubMappingAddedOrUpdated(StubMapping mapping) {
        if (mapping.isInScenario()) {
            String scenarioName = mapping.getScenarioName();
            Scenario scenario = firstNonNull(scenarioMap.get(scenarioName), Scenario.inStartedState(scenarioName));
            scenario = scenario.withPossibleState(mapping.getNewScenarioState());
            scenarioMap.put(scenarioName, scenario);
        }

    }

    public Scenario getByName(String name) {
        return scenarioMap.get(name);
    }

    public List<Scenario> getAll() {
        return ImmutableList.copyOf(scenarioMap.values());
    }

    public void onStubMappingRemoved(StubMapping mapping, Iterable<StubMapping> remainingStubMappings) {
        if (mapping.isInScenario()) {
            final String scenarioName = mapping.getScenarioName();

            int numberOfOtherStubsInThisScenario = from(remainingStubMappings).filter(new Predicate<StubMapping>() {
                @Override
                public boolean apply(StubMapping input) {
                    return input.getScenarioName().equals(scenarioName);
                }
            }).size();

            if (numberOfOtherStubsInThisScenario == 0) {
                scenarioMap.remove(scenarioName);
            } else {
                Scenario scenario = scenarioMap.get(scenarioName);
                scenario = scenario.withoutPossibleState(mapping.getNewScenarioState());
                scenarioMap.put(scenarioName, scenario);
            }
        }
    }

    public void onStubServed(StubMapping mapping) {
        if (mapping.isInScenario()) {
            final String scenarioName = mapping.getScenarioName();
            Scenario scenario = scenarioMap.get(scenarioName);
            if (mapping.modifiesScenarioState() &&
                scenario.getState().equals(mapping.getRequiredScenarioState())) {
                Scenario newScenario = scenario.setState(mapping.getNewScenarioState());
                scenarioMap.put(scenarioName, newScenario);
            }
        }
    }

    public void reset() {
        scenarioMap.putAll(Maps.transformValues(scenarioMap, new Function<Scenario, Scenario>() {
            @Override
            public Scenario apply(Scenario input) {
                return input.reset();
            }
        }));
    }

    public void clear() {
        scenarioMap.clear();
    }

    public boolean mappingMatchesScenarioState(StubMapping mapping) {
        String currentScenarioState = getByName(mapping.getScenarioName()).getState();
        return mapping.getRequiredScenarioState().equals(currentScenarioState);
    }
}
