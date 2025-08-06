package org.example.howareyou.domain.member.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.howareyou.domain.member.entity.Category;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class FilterRequest {
    private Set<Category> interests;
}
