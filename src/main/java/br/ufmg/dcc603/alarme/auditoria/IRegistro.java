package br.ufmg.dcc603.alarme.auditoria;

import java.util.List;

/**
 * Interface arquitetural do componente de Auditoria/Log (Figura 2 - Sprint 2).
 */
public interface IRegistro {

    void registrar(Evento novoEvento);

    List<Evento> listar();
}
