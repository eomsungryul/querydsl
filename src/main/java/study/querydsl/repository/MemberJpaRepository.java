
package study.querydsl.repository;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
//import study.querydsl.entity.QMember;

@Repository
public class MemberJpaRepository {
	
	private final EntityManager em;
	private final JPAQueryFactory queryFactory;
	
	//주입받는게 하나라 테스트 쿼리 쓸때 편함 
//	public MemberJpaRepository(EntityManager em) {
//		this.em = em;
//		this.queryFactory = new JPAQueryFactory(em);
//	}
	
	//팩토리 빈 등록하면
	public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
		this.em = em;
		this.queryFactory = queryFactory;
	}//이렇게 가능하고 @requmentagument 가능 
	//근데 빈등록하면 싱글톤이라 동시성 문제 되지않나?
	//트랜잭션 단위로 다 ㄷ동작하기 때문에 상관없음 
	
	public void save(Member member) {
		em.persist(member);
	}
	public Optional<Member> findById(Long id) {
		Member findMember = em.find(Member.class, id);
		//null인 경우를 대비하여 반환을 바로안한다. 
		return Optional.ofNullable(findMember);
	}
	
	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class)
				.getResultList();
	}
	
	public List<Member> findAll_Querydsl() {
		return queryFactory
				.selectFrom(member).fetch();
	}
	
	public List<Member> findByUsername(String username) {
		return em.createQuery("select m from Member m where m.username = :username", 
				Member.class)
				.setParameter("username", username)
				.getResultList();
	}
	
	public List<Member> findByUsername_Querydsl(String username) {
		return queryFactory
				.selectFrom(member)
				.where(member.username.eq(username))
				.fetch();
	}
	
	//Builder 사용
	//회원명, 팀명, 나이(ageGoe, ageLoe)
	public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
		BooleanBuilder builder = new BooleanBuilder();
		if (StringUtils.hasText(condition.getUsername())) {
			builder.and(member.username.eq(condition.getUsername()));
		}
		if (StringUtils.hasText(condition.getTeamName())) {
			builder.and(team.name.eq(condition.getTeamName()));
		}
		if (condition.getAgeGoe() != null) {
			builder.and(member.age.goe(condition.getAgeGoe()));
		}
		if (condition.getAgeLoe() != null) {
			builder.and(member.age.loe(condition.getAgeLoe()));
		}
		return queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"),
						member.username,
						member.age,
						team.id.as("teamId"),
						team.name.as("teamName")))
				.from(member)
				.leftJoin(member.team, team)
				.where(builder)
				.fetch();
	}
	
	//회원명, 팀명, 나이(ageGoe, ageLoe)
	public List<MemberTeamDto> search(MemberSearchCondition condition) {
		return queryFactory
				.select(new QMemberTeamDto(
						member.id.as("memberId"),
						member.username,
						member.age,
						team.id.as("teamId"),
						team.name.as("teamName")))
				.from(member)
				.leftJoin(member.team, team)
				.where(usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe()))
				.fetch();
	}
	
	private BooleanExpression usernameEq(String username) {
		return StringUtils.isEmpty(username) ? null : member.username.eq(username);
	}
	
	private BooleanExpression teamNameEq(String teamName) {
		return StringUtils.isEmpty(teamName) ? null : team.name.eq(teamName);
	}
	
	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe == null ? null : member.age.goe(ageGoe);
	}
	
	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe == null ? null : member.age.loe(ageLoe);
	}
	
	//where 파라미터 방식은 이런식으로 재사용이 가능하다.
	public List<Member> findMember(MemberSearchCondition condition) {
		return queryFactory
				.selectFrom(member)
				.leftJoin(member.team, team)
				.where(usernameEq(condition.getUsername()),
						teamNameEq(condition.getTeamName()),
						ageGoe(condition.getAgeGoe()),
						ageLoe(condition.getAgeLoe()))
				.fetch();
	}
	
	// 빌더로 쓸때도있지만 주로 BooleanExpression 로 써라
	
}

