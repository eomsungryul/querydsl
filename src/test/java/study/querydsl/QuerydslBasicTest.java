package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
//이런식으로 생략 가능
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@PersistenceContext
	EntityManager em;

	JPAQueryFactory queryFactory;

	@BeforeEach
	public void before() {
		queryFactory = new JPAQueryFactory(em);

		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);
		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
	}

	@Test
	public void startJPQL() {
		// member1을 찾아라.
		String qlString = "select m from Member m " + "where m.username = :username";
		Member findMember = em.createQuery(qlString, Member.class).setParameter("username", "member1")
				.getSingleResult();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl() {
		// member1을 찾아라.
		//querydsl 은 JPAQueryFactory로 시작해야함
//		JPAQueryFactory queryFactory = new JPAQueryFactory(em);
		QMember m = new QMember("m"); //m 은 별칭 근데 구분하는 이름인데 중요하지않음  알리아스가 자동적으로 먹힘
//		QMember qMember = QMember.member; //기본 인스턴스 사용

		Member findMember = queryFactory
						.select(m)
						.from(m)
						.where(m.username.eq("member1"))// 파라미터 바인딩 처리
						.fetchOne();


		//이런식으로도 가능..!
//		import static study.querydsl.entity.QMember.*;
//		 //member1을 찾아라.
//		 Member findMember = queryFactory
//							 .select(member)
//							 .from(member)
//							 .where(member.username.eq("member1"))
//							 .fetchOne();
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void search() {

		 QMember member = QMember.member;
		 Member findMember = queryFactory
						 .selectFrom(member)
						 .where(member.username.eq("member1")
						 .and(member.age.eq(10)))
						 .fetchOne();
		 //검색조건들!
//		 member.username.eq("member1") // username = 'member1'
//		 member.username.ne("member1") //username != 'member1'
//		 member.username.eq("member1").not() // username != 'member1'
//		 member.username.isNotNull() //이름이 is not null
//		 member.age.in(10, 20) // age in (10,20)
//		 member.age.notIn(10, 20) // age not in (10, 20)
//		 member.age.between(10,30) //between 10, 30
//		 member.age.goe(30) // age >= 30
//		 member.age.gt(30) // age > 30
//		 member.age.loe(30) // age <= 30
//		 member.age.lt(30) // age < 30
//		 member.username.like("member%") //like 검색
//		 member.username.contains("member") // like ‘%member%’ 검색
//		 member.username.startsWith("member") //like ‘member%’ 검색

		 assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void searchAndParam() {
		//where() 에 파라미터로 검색조건을 추가하면 AND 조건이 추가됨
	 QMember member = QMember.member;
	 List<Member> result1 = queryFactory
					 .selectFrom(member)
					 .where(member.username.eq("member1"),
					 member.age.eq(10))
					 .fetch();
	 assertThat(result1.size()).isEqualTo(1);
	}

	//결과 조회
//	fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
//	fetchOne() : 단 건 조회
//	결과가 없으면 : null
//	결과가 둘 이상이면 : com.querydsl.core.NonUniqueResultException
//	fetchFirst() : limit(1).fetchOne()
//	fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
//	fetchCount() : count 쿼리로 변경해서 count 수 조회

	@Test
	public void resultSelect() {

	 List<Member> fetch = queryFactory
			 .selectFrom(member)
			 .fetch();

	//단 건
	 Member findMember1 = queryFactory
			 .selectFrom(member)
			 .fetchOne();

	//처음 한 건 조회
	 Member findMember2 = queryFactory
			 .selectFrom(member)
			 .fetchFirst();

	//페이징에서 사용
	 QueryResults<Member> results = queryFactory
			 .selectFrom(member)
			 .fetchResults();

	//count 쿼리로 변경
	 long count = queryFactory
			 .selectFrom(member)
			 .fetchCount();
	}



	/**
	 * 회원 정렬 순서
	 * 1. 회원 나이 내림차순(desc)
	 * 2. 회원 이름 올림차순(asc)
	 * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
	 */
	@Test
	public void sort() {

		 em.persist(new Member(null, 100));
		 em.persist(new Member("member5", 100));
		 em.persist(new Member("member6", 100));
		 List<Member> result = queryFactory
							 .selectFrom(member)
							 .where(member.age.eq(100))
							 .orderBy(member.age.desc(), member.username.asc().nullsLast())
							 .fetch();
		 Member member5 = result.get(0);
		 Member member6 = result.get(1);
		 Member memberNull = result.get(2);
		 assertThat(member5.getUsername()).isEqualTo("member5");
		 assertThat(member6.getUsername()).isEqualTo("member6");
		 assertThat(memberNull.getUsername()).isNull();
	}

	//페이징
	@Test
	public void paging1() {

		 List<Member> result = queryFactory
							 .selectFrom(member)
							 .orderBy(member.username.desc())
							 .offset(1) //0부터 시작(zero index)
							 .limit(2) //최대 2건 조회
							 .fetch();

		 assertThat(result.size()).isEqualTo(2);
	}

	//전체 조회 수가 필요하면?
	@Test
	public void paging2() {

		 QueryResults<Member> queryResults = queryFactory
											 .selectFrom(member)
											 .orderBy(member.username.desc())
											 .offset(1)
											 .limit(2)
											 .fetchResults();

		 assertThat(queryResults.getTotal()).isEqualTo(4);
		 assertThat(queryResults.getLimit()).isEqualTo(2);
		 assertThat(queryResults.getOffset()).isEqualTo(1);
		 assertThat(queryResults.getResults().size()).isEqualTo(2);
		 //실무에서는 조인이 많을때는 구지 count까지 조인을 많이 할 필요가없음 성능이 별로 ㅎㅎ
		 //count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면, count 전용 쿼리를 별도로 작성해야 한다.
	}


	/**
	 * JPQL
	 * select
	 * COUNT(m), //회원수
	 * SUM(m.age), //나이 합
	 * AVG(m.age), //평균 나이
	 * MAX(m.age), //최대 나이
	 * MIN(m.age) //최소 나이
	 * from Member m
	 */
	@Test
	public void aggregation() throws Exception {

		 List<Tuple> result = queryFactory
							 .select(member.count(),
							 member.age.sum(),
							 member.age.avg(),
							 member.age.max(),
							 member.age.min())
							 .from(member)
							 .fetch();
		 //쿼리dsl에서 가져오는 튜플
		 Tuple tuple = result.get(0);

		 assertThat(tuple.get(member.count())).isEqualTo(4);
		 assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		 assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		 assertThat(tuple.get(member.age.max())).isEqualTo(40);
		 assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라.
	 */
	@Test
	public void group() throws Exception {

		 List<Tuple> result = queryFactory
							 .select(team.name, member.age.avg())
							 .from(member)
							 .join(member.team, team)
							 .groupBy(team.name)
							 .fetch();

		 Tuple teamA = result.get(0);
		 Tuple teamB = result.get(1);

		 assertThat(teamA.get(team.name)).isEqualTo("teamA");
		 assertThat(teamA.get(member.age.avg())).isEqualTo(15);
		 assertThat(teamB.get(team.name)).isEqualTo("teamB");
		 assertThat(teamB.get(member.age.avg())).isEqualTo(35);

		 //groupBy(), having() 예시
//		 .groupBy(item.price)
//		 .having(item.price.gt(1000))

	}

	//-------------------------------------------------------------------------------------
	//조인 - 기본 조인
	//-------------------------------------------------------------------------------------


	/**
	 * 팀 A에 소속된 모든 회원
	 */
	@Test
	public void join() throws Exception {

		 QMember member = QMember.member;
		 QTeam team = QTeam.team;

		 List<Member> result = queryFactory
							 .selectFrom(member)
							 .join(member.team, team)
							 .where(team.name.eq("teamA"))
							 .fetch();

		 assertThat(result)
		 .extracting("username")
		 .containsExactly("member1", "member2");

//		 join() , innerJoin() : 내부 조인(inner join)
//		 leftJoin() : left 외부 조인(left outer join)
//		 rightJoin() : rigth 외부 조인(rigth outer join)
//		 JPQL의 on 과 성능 최적화를 위한 fetch 조인 제공 다음 on 절에서 설명
	}


	/**
	 * 세타 조인(연관관계가 없는 필드로 조인)
	 * 회원의 이름이 팀 이름과 같은 회원 조회
	 */
	@Test
	public void theta_join() throws Exception {

		 em.persist(new Member("teamA"));
		 em.persist(new Member("teamB"));

		 List<Member> result = queryFactory
							 .select(member)
							 .from(member, team)
							 .where(member.username.eq(team.name))
							 .fetch();

		 assertThat(result)
		 .extracting("username")
		 .containsExactly("teamA", "teamB");

//		 from 절에 여러 엔티티를 선택해서 세타 조인
//		 외부 조인 불가능 다음에 설명할 조인 on을 사용하면 외부 조인 가능
	}

	//-------------------------------------------------------------------------------------
	//조인 - on절 (JPA 2.1부터 지원)
	//-------------------------------------------------------------------------------------

	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
	 * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
	 * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
	t.name='teamA'
	 */
	@Test
	public void join_on_filtering() throws Exception {

		 List<Tuple> result = queryFactory
							 .select(member, team)
							 .from(member)
							 .leftJoin(member.team, team).on(team.name.eq("teamA"))
							 .fetch();

		 for (Tuple tuple : result) {
			 System.out.println("tuple = " + tuple);
		 }

//		 참고: on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면,
//		 where 절에서 필터링 하는 것과 기능이 동일하다. 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때,
//		 내부조인 이면 익숙한 where 절로 해결하고, 정말 외부조인이 필요한 경우에만 이 기능을 사용하자
	}

	/**
	 * 2. 연관관계 없는 엔티티 외부 조인
	 * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
	 * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
	 * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
	 */
	@Test
	public void join_on_no_relation() throws Exception {

		 em.persist(new Member("teamA"));
		 em.persist(new Member("teamB"));

		 List<Tuple> result = queryFactory
							 .select(member, team)
							 .from(member)
							 .leftJoin(team).on(member.username.eq(team.name)) //on절은 필터링 대상을 줄이는 기능
							 .fetch();

		 for (Tuple tuple : result) {
			 System.out.println("t=" + tuple);
		 }

//		 하이버네이트 5.1부터 on 을 사용해서 서로 관계가 없는 필드로 외부 조인하는 기능이 추가되었다. 물론 내
//		 부 조인도 가능하다.
//		 주의! 문법을 잘 봐야 한다. leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
//		 일반조인: leftJoin(member.team, team)
//		 on조인: from(member).leftJoin(team).on(xxx)

	}



	//-------------------------------------------------------------------------------------
	//조인 - 페치 조인
	//-------------------------------------------------------------------------------------

	@PersistenceUnit
	EntityManagerFactory emf;


	@Test
	public void fetchJoinNo() throws Exception {

		 em.flush();
		 em.clear();

		 Member findMember = queryFactory
							 .selectFrom(member)
							 .where(member.username.eq("member1"))
							 .fetchOne();

		 boolean loaded =
		 emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); //얘가 로딩된 엔티티인지 아닌지
		 assertThat(loaded).as("페치 조인 미적용").isFalse();
	}

	@Test
	public void fetchJoinUse() throws Exception {

		 em.flush();
		 em.clear();

		 Member findMember = queryFactory
							 .selectFrom(member)
							 .join(member.team, team).fetchJoin() //페치조인 추가만 하면됨
							 .where(member.username.eq("member1"))
							 .fetchOne();

		 boolean loaded =
		 emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		 assertThat(loaded).as("페치 조인 적용").isTrue();
	}




	//-------------------------------------------------------------------------------------
	//서브 쿼리
	//-------------------------------------------------------------------------------------

	/**
	 * 나이가 가장 많은 회원 조회
	 */
	@Test
	public void subQuery() throws Exception {
		
		//서브쿼리는 바깥에있는 알리아스와 겹치면 안됨 그래서 서브로 사용
		 QMember memberSub = new QMember("memberSub");
		 //서브쿼리는 JPAExpressions 사용
		 
		 List<Member> result = queryFactory
							 .selectFrom(member)
							 .where(member.age.eq(
								 JPAExpressions
								 .select(memberSub.age.max())
								 .from(memberSub)
							 ))
							 .fetch();
		 
		 assertThat(result).extracting("age")
		 .containsExactly(40);
	}
	
	/**
	 * 나이가 평균 나이 이상인 회원
	 */
	@Test
	public void subQueryGoe() throws Exception {
		//GOE >=
		QMember memberSub = new QMember("memberSub");
		
		 List<Member> result = queryFactory
							 .selectFrom(member)
							 .where(member.age.goe(
								 JPAExpressions
								 .select(memberSub.age.avg())
								 .from(memberSub)
							 ))
							 .fetch();
		 
		 assertThat(result).extracting("age")
		 .containsExactly(30,40);
	}
	
	/**
	 * 서브쿼리 여러 건 처리, in 사용
	 */
	@Test
	public void subQueryIn() throws Exception {
		 //gt > 임
		 QMember memberSub = new QMember("memberSub");
		 
		 List<Member> result = queryFactory
							 .selectFrom(member)
							 .where(member.age.in(
								 JPAExpressions
								 .select(memberSub.age)
								 .from(memberSub)
								 .where(memberSub.age.gt(10))
							 ))
							 .fetch();
		 
		 assertThat(result).extracting("age")
		 .containsExactly(20, 30, 40);
	}
	

	/**
	 * select 절에 subquery
	 */
	@Test
	public void selectSubQuery() throws Exception {

		QMember memberSub = new QMember("memberSub");

		List<Tuple> fetch = queryFactory
							 .select(member.username,
								 JPAExpressions
								 .select(memberSub.age.avg())
								 .from(memberSub)
							 ).from(member)
							 .fetch();
		
		for (Tuple tuple : fetch) {
			 System.out.println("username = " + tuple.get(member.username));
			 System.out.println("age = " +
			 tuple.get(JPAExpressions.select(memberSub.age.avg())
			 .from(memberSub)));
		}
		
		
		//이런식으로도 활용 
//		import static com.querydsl.jpa.JPAExpressions.select;
//		List<Member> result = queryFactory
//		 .selectFrom(member)
//		 .where(member.age.eq(
//		 select(memberSub.age.max())
//		 .from(memberSub)
//		 ))
//		 .fetch();
		
		
		//from 절의 서브쿼리 한계
//		JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다. 당연히 Querydsl
//		도 지원하지 않는다. 하이버네이트 구현체를 사용하면 select 절의 서브쿼리는 지원한다. Querydsl도 하
//		이버네이트 구현체를 사용하면 select 절의 서브쿼리를 지원한
		
		//from 절의 서브쿼리 해결방안
//		1. 서브쿼리를 join으로 변경한다. (가능한 상황도 있고, 불가능한 상황도 있다.)
//		2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
//		3. nativeSQL을 사용한다
	}
	


	//-------------------------------------------------------------------------------------
	//Case 문
	//-------------------------------------------------------------------------------------


	/**
	 * 단순한 조건
	 */
	@Test
	public void basicCase() throws Exception {
		
		List<String> result = queryFactory
				.select(member.age
						.when(10).then("열살")
						.when(20).then("스무살")
						.otherwise("기타"))
				.from(member)
				.fetch();
		
		for(String s : result) {
			System.out.println("s : "+s);
		}
		
		
	}
	


	/**
	 * 복잡한 조건 
	 */
	@Test
	public void complexCase() throws Exception {
		
		List<String> result = queryFactory
				.select(new CaseBuilder()
						.when(member.age.between(0, 20)).then("0~20살")
						.when(member.age.between(21, 30)).then("21~30살")
						.otherwise("기타"))
				.from(member)
				.fetch();
		
		for(String s : result) {
			System.out.println("s : "+s);
		}
	}
	
	//Case문은 무조건 필터링을 하는 부분은 DB에서 안하고 애플리케이션에서 하는게좋음 


	//-------------------------------------------------------------------------------------
	//상수, 문자더하기
	//-------------------------------------------------------------------------------------

	//Expressions.constant(xxx) 사용
	

	/**
	 * 상수
	 */
	@Test
	public void constant() throws Exception {
		
		List<Tuple> result = queryFactory
							.select(member.username, Expressions.constant("A"))
							.from(member)
							.fetch();
		
		for(Tuple s : result) {
			System.out.println("s : "+s);
		}
	}
	

	/**
	 * 문자 더하기 concat
	 */
	@Test
	public void concat() throws Exception {
		
		List<String> result = queryFactory
				.select(member.username.concat("_").concat(member.age.stringValue()))
				.from(member)
				.where(member.username.eq("member1"))
				.fetch();

		for(String s : result) {
			System.out.println("s : "+s);
		}
		
	}
	
	//====================================================================================
	//중급 문법
	//====================================================================================
	
	//-------------------------------------------------------------------------------------
	//프로젝션과 결과 반환 - 기본
	//-------------------------------------------------------------------------------------

	//	프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음
	//	프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회
	@Test
	public void simpleProjection() throws Exception {
		
		List<String> result = queryFactory
				 .select(member.username)
				 .from(member)
				 .fetch();

		for(String s : result) {
			System.out.println("s : "+s);
		}
	}
	
	
	@Test
	public void tupleProjection() throws Exception {
		
		List<Tuple> result = queryFactory
				.select(member.username, member.age)
				.from(member)
				.fetch();
		for (Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);
			System.out.println("username=" + username);
			System.out.println("age=" + age);
		}
		//튜플은 리파지토리내에만 쓰는것으로..
		
	}


	//-------------------------------------------------------------------------------------
	//프로젝션과 결과 반환 - DTO 조회
	//-------------------------------------------------------------------------------------
	
	//순수 JPA에서 DTO 조회 코드
	@Test
	public void findDtoByJPQL() throws Exception {
		List<MemberDto> result = em.createQuery(
				 "select new study.querydsl.dto.MemberDto(m.username, m.age) " +
				 "from Member m", MemberDto.class)
				 .getResultList();
		
		for(MemberDto mDto: result ) {
			System.out.println("mDto : "+mDto);
		}
	}
	
	//	qdsl은 세가지 방법 지원 
	//	1.프로퍼티 접근 - Setter
	@Test
	public void findDtoBySetter() throws Exception {
		List<MemberDto> result = queryFactory
				.select(Projections.bean(MemberDto.class,
						member.username,
						member.age))
				.from(member)
				.fetch();

		for(MemberDto mDto: result ) {
			System.out.println("mDto : "+mDto);
		}
	}
	
	//	2.필드 직접 접근 게터세터 무시하ㅗㄱㄷ 바로 박음

	public void findDtoByField() throws Exception {
		List<MemberDto> result = queryFactory
				.select(Projections.fields(MemberDto.class,
						member.username,
						member.age))
				.from(member)
				.fetch();

		for(MemberDto mDto: result ) {
			System.out.println("mDto : "+mDto);
		}
	}
	
	//	3.생성자 사용

	public void findDtoByConstructor() throws Exception {
		List<MemberDto> result = queryFactory
				.select(Projections.constructor(MemberDto.class,
						member.username,
						member.age))
				.from(member)
				.fetch();

		for(MemberDto mDto: result ) {
			System.out.println("mDto : "+mDto);
		}
	}
	//번외 - 별칭이 다를 경우
	//	프로퍼티나, 필드 접근 생성 방식에서 이름이 다를 때 해결 방안
	//	ExpressionUtils.as(source,alias) : 필드나, 서브 쿼리에 별칭 적용
	//	username.as("memberName") : 필드에 별칭 적용

	//-------------------------------------------------------------------------------------
	//프로젝션과 결과 반환 - @QueryProjection
	//-------------------------------------------------------------------------------------

	public void findDtoByQueryProjection() throws Exception {
		List<MemberDto> result = queryFactory
				.select(new QMemberDto(member.username, member.age))
				.from(member)
				.fetch();

		for(MemberDto mDto: result ) {
			System.out.println("mDto : "+mDto);
		}
		//컨스트럭터랑 같은데 컴파일 오류를 잡을수 있다
		// 근데 실무에서는 컴파일도되서 가장 안정한 방법이긴한데... 
		// 단점 애노테이션 넣어야되고 dto에 querydsl 의존성이 생김 
	}
	
	//-------------------------------------------------------------------------------------
	//동적 쿼리 - BooleanBuilder 사용
	//-------------------------------------------------------------------------------------
	
	//	동적 쿼리를 해결하는 두가지 방식
	//	BooleanBuilder
	//	Where 다중 파라미터 사용
	@Test
	public void dynamicQuery_BooleanBuilder() throws Exception {
		String usernameParam = "member1";
		Integer ageParam = 10;
		List<Member> result = searchMember1(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}
	
	private List<Member> searchMember1(String usernameCond, Integer ageCond) {
		BooleanBuilder builder = new BooleanBuilder();
		if (usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}
		if (ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}
		return queryFactory
				.selectFrom(member)
				.where(builder)
				.fetch();
	}
	
	
	//-------------------------------------------------------------------------------------
	//동적 쿼리 - Where 다중 파라미터 사용
	//-------------------------------------------------------------------------------------
	
	//실무에서 자주 사용하는 방법 
	@Test
	public void dynamicQuery_WhereParam() throws Exception {
		String usernameParam = "member1";
		Integer ageParam = 10;
		List<Member> result = searchMember2(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}
	
	private List<Member> searchMember2(String usernameCond, Integer ageCond) {
		return queryFactory
				.selectFrom(member)
//				.where(usernameEq(usernameCond), ageEq(ageCond))
				.where(allEq(usernameCond, ageCond))
				.fetch();
	}
	
	private BooleanExpression usernameEq(String usernameCond) {
		return usernameCond != null ? member.username.eq(usernameCond) : null;
	}
	
	private BooleanExpression ageEq(Integer ageCond) {
		return ageCond != null ? member.age.eq(ageCond) : null;
	}
	//조합도 가능! 	
	private BooleanExpression allEq(String usernameCond, Integer ageCond) {
		return usernameEq(usernameCond).and(ageEq(ageCond));
	}
	//광고가 상태가 isValid 날짜가 IN : isServiceable  
	
	//실무할때는 이렇게 메서드 형태를 재활용할수있어서 개쩌는듯..
	
	//-------------------------------------------------------------------------------------
	//수정, 삭제 벌크 연산
	//-------------------------------------------------------------------------------------
	
	@Test
	public void bulkUpdate() throws Exception {
		
		
		//28살보다 아래면 비회원
		long count = queryFactory
				.update(member)
				.set(member.username, "비회원")
				.where(member.age.lt(28))
				.execute();
		//벌크는 DB로 바로쏘기때문에 영속성 컨텍스트에는 안바뀌기에 
		//둘이 상태가 안맞음 그래서
		em.flush();
		em.clear();
		//초기화해야함
	}

	@Test
	public void bulkAdd() throws Exception {
		//기존 숫자에 1 더하기
		long count = queryFactory
				.update(member)
				.set(member.age, member.age.add(1))
				.execute();
		
		//곱하기는 multiply(x)
	}

	@Test
	public void bulkDelete() throws Exception {
		//18살 이상의 모든 회원을 지운다..
		long count = queryFactory
				.delete(member)
				.where(member.age.gt(18))
				.execute();
	}
	
	
	//-------------------------------------------------------------------------------------
	//SQL function 호출하기
	//-------------------------------------------------------------------------------------
	

	@Test
	public void sqlFunction() throws Exception {
		
		//member > M으로 변경하는 replace 함수 사용
		List<String> result = queryFactory
				.select(Expressions.stringTemplate("function('replace', {0}, {1},{2})", 
						member.username, "member", "M"))
						.from(member)
						.fetch();
		
	}
	
	@Test
	public void sqlFunction2() throws Exception {
		
		//소문자로 변경해서 비교해라
		List<String> result = queryFactory
				.select(member.username)
				.from(member)
//				.where(member.username.eq(Expressions.stringTemplate("function('lower', {0})",
//						member.username)))
				.where(member.username.eq(member.username.lower()))
				.fetch();
		
	}
	
	
	
	
	
	
	
	

	//====================================================================================
	//실무 활용 - 순수 JPA와 Querydsl
	//====================================================================================
	
	//-------------------------------------------------------------------------------------
	//순수 JPA 리포지토리와 Querydsl
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//동적 쿼리와 성능 최적화 조회 - Builder 사용
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//동적 쿼리와 성능 최적화 조회 - Where절 파라미터 사용
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//조회 API 컨트롤러 개발
	//-------------------------------------------------------------------------------------


	//====================================================================================
	//실무 활용 - 스프링 데이터 JPA와 Querydsl
	//====================================================================================

	//-------------------------------------------------------------------------------------
	//스프링 데이터 JPA 리포지토리로 변경
	//-------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------
	//사용자 정의 리포지토리
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//스프링 데이터 페이징 활용1 - Querydsl 페이징 연동
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//스프링 데이터 페이징 활용2 - CountQuery 최적화
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//스프링 데이터 페이징 활용3 - 컨트롤러 개발
	//-------------------------------------------------------------------------------------

	//====================================================================================
	//스프링 데이터 JPA가 제공하는 Querydsl 기능
	//====================================================================================

	//-------------------------------------------------------------------------------------
	//인터페이스 지원 - QuerydslPredicateExecutor
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//Querydsl Web 지원
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//리포지토리 지원 - QuerydslRepositorySupport
	//-------------------------------------------------------------------------------------
	
	//-------------------------------------------------------------------------------------
	//Querydsl 지원 클래스 직접 만들기
	//-------------------------------------------------------------------------------------


}