package com.buddy.common.statemachine;

import com.buddy.common.constant.PostStatus;
import com.buddy.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 帖子状态机单元测试：逐边验证合法流转与非法拦截
 */
class PostStateMachineTest {

    @Test
    void recruitingCanGoToFullOngoingCancelled() {
        assertThat(PostStateMachine.canTransit(PostStatus.RECRUITING, PostStatus.FULL)).isTrue();
        assertThat(PostStateMachine.canTransit(PostStatus.RECRUITING, PostStatus.ONGOING)).isTrue();
        assertThat(PostStateMachine.canTransit(PostStatus.RECRUITING, PostStatus.CANCELLED)).isTrue();
    }

    @Test
    void fullCanBackToRecruitingAndForward() {
        assertThat(PostStateMachine.canTransit(PostStatus.FULL, PostStatus.RECRUITING)).isTrue();
        assertThat(PostStateMachine.canTransit(PostStatus.FULL, PostStatus.ONGOING)).isTrue();
        assertThat(PostStateMachine.canTransit(PostStatus.FULL, PostStatus.CANCELLED)).isTrue();
    }

    @Test
    void ongoingCanOnlyFinishOrCancel() {
        assertThat(PostStateMachine.canTransit(PostStatus.ONGOING, PostStatus.FINISHED)).isTrue();
        assertThat(PostStateMachine.canTransit(PostStatus.ONGOING, PostStatus.CANCELLED)).isTrue();
        assertThat(PostStateMachine.canTransit(PostStatus.ONGOING, PostStatus.RECRUITING)).isFalse();
        assertThat(PostStateMachine.canTransit(PostStatus.ONGOING, PostStatus.FULL)).isFalse();
    }

    @Test
    void finishedAndCancelledAreTerminal() {
        for (String target : new String[]{PostStatus.RECRUITING, PostStatus.FULL, PostStatus.ONGOING, PostStatus.FINISHED, PostStatus.CANCELLED}) {
            assertThat(PostStateMachine.canTransit(PostStatus.FINISHED, target))
                    .as("FINISHED -> " + target).isFalse();
            assertThat(PostStateMachine.canTransit(PostStatus.CANCELLED, target))
                    .as("CANCELLED -> " + target).isFalse();
        }
    }

    @Test
    void illegalTransitionThrows() {
        assertThatThrownBy(() -> PostStateMachine.checkTransition(PostStatus.RECRUITING, PostStatus.FINISHED))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PostStateMachine.checkTransition(PostStatus.FINISHED, PostStatus.RECRUITING))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PostStateMachine.checkTransition(PostStatus.CANCELLED, PostStatus.ONGOING))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void legalTransitionDoesNotThrow() {
        PostStateMachine.checkTransition(PostStatus.RECRUITING, PostStatus.FULL);
        PostStateMachine.checkTransition(PostStatus.FULL, PostStatus.ONGOING);
        PostStateMachine.checkTransition(PostStatus.ONGOING, PostStatus.FINISHED);
        assertThat(true).isTrue();
    }

    @Test
    void unknownStatusIsRejected() {
        assertThat(PostStateMachine.canTransit("UNKNOWN", PostStatus.RECRUITING)).isFalse();
        assertThatThrownBy(() -> PostStateMachine.checkTransition("UNKNOWN", PostStatus.RECRUITING))
                .isInstanceOf(BusinessException.class);
    }
}
