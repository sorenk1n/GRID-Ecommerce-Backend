package com.khomsi.backend.main.utils.email.service.impl;

import com.khomsi.backend.main.checkout.model.entity.Transaction;
import com.khomsi.backend.main.game.model.dto.ShortGameModel;
import com.khomsi.backend.main.user.model.entity.UserInfo;
import com.khomsi.backend.main.utils.email.service.EmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEmailService implements EmailService {
    @Override
    public void sendPurchaseConfirmationEmail(Transaction transaction) {
        // no-op in dev
    }

    @Override
    public void sendBalanceUpdateNotification(String email, BigDecimal oldBalance, BigDecimal newBalance) {
        // no-op in dev
    }

    @Override
    public void sendDiscountNotificationEmail(List<ShortGameModel> discountedGames, UserInfo user) {
        // no-op in dev
    }

    @Override
    public void sendWarningEmail(String notification, UserInfo user) {
        // no-op in dev
    }
}
