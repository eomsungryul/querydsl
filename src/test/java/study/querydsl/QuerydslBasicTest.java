package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

//이런식으로 생략 가능
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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
	

}