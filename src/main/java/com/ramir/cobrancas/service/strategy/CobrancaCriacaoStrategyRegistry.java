package com.ramir.cobrancas.service.strategy;

import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CobrancaCriacaoStrategyRegistry {

    private final Map<CobrancaMetodoEnum, CobrancaCriacaoStrategy> map;

    public CobrancaCriacaoStrategyRegistry(List<CobrancaCriacaoStrategy> list) {
        map = list.stream()
                .collect(Collectors.toMap(
                        CobrancaCriacaoStrategy::metodo, Function.identity()));
    }

    public CobrancaCriacaoStrategy get(CobrancaMetodoEnum metodo) {
        return map.get(metodo);
    }
}
