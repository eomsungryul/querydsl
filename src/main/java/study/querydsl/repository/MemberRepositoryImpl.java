package study.querydsl.repository;


import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

public class MemberRepositoryImpl implements MemberRepositoryCustom {
	private final JPAQueryFactory queryFactory;
	public MemberRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}
	@Override
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
		return isEmpty(username) ? null : member.username.eq(username);
	}
	private BooleanExpression teamNameEq(String teamName) {
		return isEmpty(teamName) ? null : team.name.eq(teamName);
	}
	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe == null ? null : member.age.goe(ageGoe);
	}
	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe == null ? null : member.age.loe(ageLoe);
	}
	
	/**
	 * 단순한 페이징, fetchResults() 사용
	 */
	@Override
	public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition,
			Pageable pageable) {
		
		QueryResults<MemberTeamDto> results = queryFactory
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
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetchResults(); // 카운터랑 fetch같이 올라감 
		
		List<MemberTeamDto> content = results.getResults();
		long total = results.getTotal();
		
		return new PageImpl<>(content, pageable, total);
	}
	
	
	/**
	 * 복잡한 페이징
	 * 데이터 조회 쿼리와, 전체 카운트 쿼리를 분리
	 */
	@Override
	public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition,
			Pageable pageable) {
		List<MemberTeamDto> content = queryFactory
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
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();
		
//		long total = queryFactory
//				.select(member)
//				.from(member)
//				.leftJoin(member.team, team)
//				.where(usernameEq(condition.getUsername()),
//						teamNameEq(condition.getTeamName()),
//						ageGoe(condition.getAgeGoe()),
//						ageLoe(condition.getAgeLoe()))
//				.fetchCount();
		
		//오더바이를 지워서 조금이라도 성능 최적화가 
		
//		return new PageImpl<>(content, pageable, total);
		//선능 최적화... 
		
		JPAQuery<Member> countQuery = queryFactory
				 .select(member)
				 .from(member)
				 .leftJoin(member.team, team)
				 .where(usernameEq(condition.getUsername()),
				 teamNameEq(condition.getTeamName()),
				 ageGoe(condition.getAgeGoe()),
				 ageLoe(condition.getAgeLoe()));
		
		return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
		
		
		//스프링 데이터 라이브러리가 제공
		//count 쿼리가 생략 가능한 경우 생략해서 처리
		//페이지 시작이면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때
		//마지막 페이지 일 때 (offset + 컨텐츠 사이즈를 더해서 전체 사이즈 구함)

	}
	
	
}