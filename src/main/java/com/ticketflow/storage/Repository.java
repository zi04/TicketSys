package com.ticketflow.storage;

import io.vavr.collection.List;
import io.vavr.control.Option;

public interface Repository<T, ID> {

    Option<T> findById(ID id);
    T save(T entity);
    List<T> findAll();
}
