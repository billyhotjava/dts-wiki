package com.yuzhi.dts.wiki.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.Label} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LabelDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 50)
    private String name;

    private Set<PageDTO> pageses = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<PageDTO> getPageses() {
        return pageses;
    }

    public void setPageses(Set<PageDTO> pageses) {
        this.pageses = pageses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LabelDTO)) {
            return false;
        }

        LabelDTO labelDTO = (LabelDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, labelDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LabelDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", pageses=" + getPageses() +
            "}";
    }
}
