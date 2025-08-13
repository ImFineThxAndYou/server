package org.example.howareyou.domain.recommendationtag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.global.entity.BaseEntity;

@Entity
@Getter
@Setter
public class MemberTagScore extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="member_id", nullable=false)
  private Long memberId;

  @Column(nullable = false, length = 100)
  private Category category;

  @Column(nullable=false)
  private double score;


}
