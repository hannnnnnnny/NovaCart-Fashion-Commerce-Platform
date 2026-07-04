package com.novacart.store.service;

import com.novacart.store.dto.MessageDtos;
import com.novacart.store.entity.Conversation;
import com.novacart.store.entity.Listing;
import com.novacart.store.entity.Message;
import com.novacart.store.entity.User;
import com.novacart.store.exception.BusinessRuleException;
import com.novacart.store.exception.ForbiddenOperationException;
import com.novacart.store.exception.ResourceNotFoundException;
import com.novacart.store.repository.ConversationRepository;
import com.novacart.store.repository.MessageRepository;
import com.novacart.store.security.CurrentUserService;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingService listingService;
    private final CurrentUserService currentUserService;

    public MessagingService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ListingService listingService,
            CurrentUserService currentUserService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.listingService = listingService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public MessageDtos.ConversationDetail startOrSendInitial(MessageDtos.StartConversationRequest request) {
        User current = currentUserService.requireCurrentUser();
        Listing listing = listingService.requireById(request.listingId());
        if (listing.getSeller().getId().equals(current.getId())) {
            throw new BusinessRuleException("You cannot message yourself about your own listing.");
        }
        Conversation conversation = conversationRepository.findByListingAndBuyer(listing, current).orElseGet(() -> {
            Conversation c = new Conversation();
            c.setListing(listing);
            c.setBuyer(current);
            c.setSeller(listing.getSeller());
            c.setCreatedAt(Instant.now());
            c.setLastMessageAt(Instant.now());
            return conversationRepository.save(c);
        });
        sendMessageInternal(conversation, current, request.body());
        return detail(conversation, current);
    }

    @Transactional
    public MessageDtos.MessageResponse sendMessage(Long conversationId, MessageDtos.SendMessageRequest request) {
        User current = currentUserService.requireCurrentUser();
        Conversation conversation = requireParticipant(conversationId, current);
        Message m = sendMessageInternal(conversation, current, request.body());
        return MessageDtos.MessageResponse.from(m);
    }

    private Message sendMessageInternal(Conversation conversation, User sender, String body) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setBody(body);
        message.setCreatedAt(Instant.now());
        messageRepository.save(message);

        conversation.setLastMessageAt(message.getCreatedAt());
        conversation.setLastMessagePreview(body.length() > 280 ? body.substring(0, 277) + "..." : body);
        if (sender.getId().equals(conversation.getBuyer().getId())) {
            conversation.setSellerUnreadCount(conversation.getSellerUnreadCount() + 1);
        } else {
            conversation.setBuyerUnreadCount(conversation.getBuyerUnreadCount() + 1);
        }
        return message;
    }

    @Transactional
    public MessageDtos.ConversationDetail openConversation(Long conversationId) {
        User current = currentUserService.requireCurrentUser();
        Conversation conversation = requireParticipant(conversationId, current);
        // mark as read for current viewer
        boolean isBuyer = conversation.getBuyer().getId().equals(current.getId());
        if (isBuyer && conversation.getBuyerUnreadCount() > 0) {
            conversation.setBuyerUnreadCount(0);
        } else if (!isBuyer && conversation.getSellerUnreadCount() > 0) {
            conversation.setSellerUnreadCount(0);
        }
        for (Message m : messageRepository.findByConversationOrderByCreatedAtAsc(conversation)) {
            if (m.getReadAt() == null && !m.getSender().getId().equals(current.getId())) {
                m.setReadAt(Instant.now());
            }
        }
        return detail(conversation, current);
    }

    @Transactional(readOnly = true)
    public List<MessageDtos.ConversationSummary> listMine() {
        User current = currentUserService.requireCurrentUser();
        return conversationRepository.findAllForUser(current, PageRequest.of(0, 100)).stream()
                .map(c -> MessageDtos.ConversationSummary.from(c, current.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        User current = currentUserService.requireCurrentUser();
        return conversationRepository.totalUnreadForUser(current);
    }

    private MessageDtos.ConversationDetail detail(Conversation conversation, User current) {
        List<MessageDtos.MessageResponse> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation).stream()
                .map(MessageDtos.MessageResponse::from)
                .toList();
        return new MessageDtos.ConversationDetail(
                MessageDtos.ConversationSummary.from(conversation, current.getId()),
                messages
        );
    }

    private Conversation requireParticipant(Long id, User user) {
        Conversation c = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));
        if (!c.getBuyer().getId().equals(user.getId()) && !c.getSeller().getId().equals(user.getId())) {
            throw new ForbiddenOperationException("You are not a participant in this conversation.");
        }
        return c;
    }
}
