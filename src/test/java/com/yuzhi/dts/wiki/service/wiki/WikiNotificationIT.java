package com.yuzhi.dts.wiki.service.wiki;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.*;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Committed fixtures prove external identity and SMTP calls have no active DB transaction. */
@IntegrationTest
@TestPropertySource(properties={"application.wiki.notifications.mail-enabled=true", "application.wiki.notifications.from=wiki@example.invalid", "application.wiki.notifications.public-url=https://wiki.example.invalid"})
class WikiNotificationIT {
    @Autowired JdbcTemplate sql;
    @Autowired PageService pages;
    @Autowired PageRepository pageRepository;
    @Autowired SpaceRepository spaces;
    @Autowired UserRepository users;
    @Autowired WikiNotificationIntents intents;
    @Autowired WikiNotificationService notices;
    @Autowired WikiPersonalService personal;
    @Autowired PlatformTransactionManager manager;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @MockitoBean CurrentIdentityService identity;
    @MockitoBean JavaMailSender sender;
    String alice, bob, slug, role;
    long spaceId, page, root;
    @BeforeEach void seed() {
        String suffix = UUID.randomUUID().toString().substring(0,8); alice="na-"+suffix; bob="nb-"+suffix; slug="notice-"+suffix;
        role="ROLE_SPACE_"+slug.toUpperCase().replace('-','_'); as(alice);
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            for (String login : List.of(alice,bob)) { User user=new User(); user.setId(login); user.setLogin(login); user.setActivated(true); users.saveAndFlush(user); }
            spaceId=spaces.saveAndFlush(new Space().slug(slug).name("Notification fixture").archived(false)).getId();
            root=pages.createPage(slug,new PageDtos.CreatePageRequest(null,"Root","FOLDER",null)).id();
            page=pages.createPage(slug,new PageDtos.CreatePageRequest(root,"Knowledge","NATIVE","# Knowledge\n")).id();
        });
        when(identity.lookup(bob)).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new CurrentIdentityService.Identity(true,Set.of(role),"recipient@example.invalid");
        });
        doAnswer(call -> { assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse(); return null; }).when(sender).send(any(SimpleMailMessage.class));
    }
    @AfterEach void cleanup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            sql.update("DELETE FROM notification WHERE page_id IN (SELECT id FROM page WHERE space_id=?)",spaceId);
            sql.update("DELETE FROM page_watch WHERE page_id IN (SELECT id FROM page WHERE space_id=?)",spaceId);
            sql.update("DELETE FROM activity_event WHERE space_id=?",spaceId);
            sql.update("UPDATE page SET current_version_id=NULL WHERE space_id=?",spaceId);
            sql.update("DELETE FROM page_version WHERE page_id IN (SELECT id FROM page WHERE space_id=?)",spaceId);
            sql.update("DELETE FROM page WHERE space_id=? AND parent_id IS NOT NULL",spaceId);
            sql.update("DELETE FROM page WHERE space_id=?",spaceId); sql.update("DELETE FROM space WHERE id=?",spaceId);
            sql.update("DELETE FROM jhi_user WHERE id IN (?,?)",alice,bob);
        });
        entityManagerFactory.getCache().evictAll(); SecurityContextHolder.clearContext();
    }
    private void as(String login) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,"unused",List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
    private void version(int base, String md) { pages.saveContent(page,new PageDtos.SaveContentRequest(base,md,"Fixture update")); }
    private long count(String predicate) { return sql.queryForObject("SELECT count(*) FROM notification WHERE recipient_id=? AND "+predicate,Long.class,bob); }
    @Test void intentIsIdempotentAndOutageRetriesWithoutEmail() {
        version(1,"Hi @"+bob+" @"+bob+"\n`@ignored`\n");
        new TransactionTemplate(manager).executeWithoutResult(status -> { var current=pageRepository.findById(page).orElseThrow(); intents.version(current,current.getCurrentVersion(),alice); });
        assertThat(count("type='MENTION'")).isEqualTo(1);
        when(identity.lookup(bob)).thenThrow(new IdentityUnavailableException()); notices.process();
        assertThat(count("auth_state='PENDING' AND attempts=0")).isEqualTo(1); verifyNoInteractions(sender);
        doReturn(new CurrentIdentityService.Identity(true,Set.of(role),"recipient@example.invalid")).when(identity).lookup(bob);
        new TransactionTemplate(manager).executeWithoutResult(status -> sql.update("UPDATE notification SET retry_at=CURRENT_TIMESTAMP WHERE recipient_id=?",bob)); notices.process(); notices.process();
        assertThat(count("delivery_state='SENT'")).isEqualTo(1); verify(sender,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void revocationAndDeletionSuppressIntentAndCurrentLists() {
        version(1,"Hi @"+bob+"\n"); as(bob); assertThat(notices.list(0,20).unread()).isEqualTo(1);
        when(identity.lookup(bob)).thenReturn(new CurrentIdentityService.Identity(true,Set.of(),null));
        assertThat(notices.list(0,20).total()).isZero(); assertThatThrownBy(() -> notices.open(sql.queryForObject("SELECT id FROM notification WHERE recipient_id=?",Long.class,bob))).isInstanceOf(SpaceNotVisibleException.class);
        notices.process(); assertThat(count("auth_state='SUPPRESSED'")).isEqualTo(1); verifyNoInteractions(sender);
        as(alice); version(2,"Hi again @"+bob+"\n");
        doReturn(new CurrentIdentityService.Identity(true,Set.of(role),"recipient@example.invalid")).when(identity).lookup(bob);
        new TransactionTemplate(manager).executeWithoutResult(status -> sql.update("UPDATE page SET deleted_at=CURRENT_TIMESTAMP WHERE id=?",page)); notices.process();
        assertThat(count("auth_state='SUPPRESSED'")).isEqualTo(2); verifyNoInteractions(sender);
    }
    @Test void pageUpdatesCollapseMailWhileReadStateRemainsIndependent() {
        new TransactionTemplate(manager).executeWithoutResult(status -> intents.watch(pageRepository.findById(page).orElseThrow(),users.findById(bob).orElseThrow(),true));
        for (int i=1;i<=3;i++) version(i,"# Version "+i+"\n");
        new TransactionTemplate(manager).executeWithoutResult(status -> sql.update("UPDATE notification SET created_at=CURRENT_TIMESTAMP-interval '11 minutes' WHERE recipient_id=?",bob));
        notices.process(); notices.process();
        assertThat(count("delivery_state='SENT' AND read_at IS NULL")).isEqualTo(3); verify(sender,times(1)).send(any(SimpleMailMessage.class));
        as(bob); assertThat(notices.list(0,20).unread()).isEqualTo(3); notices.read(null); assertThat(notices.list(0,20).unread()).isZero();
    }
    @Test void mailFailureRetriesAndExpiredSendingClaimRecovers() {
        version(1,"Hi @"+bob+"\n"); doThrow(new org.springframework.mail.MailSendException("fixture failure")).when(sender).send(any(SimpleMailMessage.class));
        notices.process(); assertThat(count("delivery_state='PENDING' AND attempts=1")).isEqualTo(1);
        new TransactionTemplate(manager).executeWithoutResult(status -> sql.update("UPDATE notification SET delivery_state='SENDING',retry_at=CURRENT_TIMESTAMP-interval '1 minute' WHERE recipient_id=?",bob));
        doNothing().when(sender).send(any(SimpleMailMessage.class)); notices.process();
        assertThat(count("delivery_state='SENT'")).isEqualTo(1);
    }
    @Test void simultaneousVisitsCannotExceedFifty() throws Exception {
        var ids=new java.util.ArrayList<Long>();
        for(int i=0;i<51;i++) ids.add(pages.createPage(slug,new PageDtos.CreatePageRequest(root,"Visit "+i,"NATIVE",null)).id());
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var tasks=ids.stream().map(id -> pool.submit(() -> { as(bob); try { personal.view(id); } finally { SecurityContextHolder.clearContext(); } })).toList();
            for(var task:tasks) task.get(30,java.util.concurrent.TimeUnit.SECONDS);
        }
        as(bob); assertThat(personal.list(false,0,50).total()).isEqualTo(50);
        assertThat(sql.queryForObject("SELECT count(*) FROM page_view WHERE user_id=?",Long.class,bob)).isEqualTo(50);
    }
}
