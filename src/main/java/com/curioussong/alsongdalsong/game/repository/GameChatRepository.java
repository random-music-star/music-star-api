package com.curioussong.alsongdalsong.game.repository;

import com.curioussong.alsongdalsong.game.domain.GameChat;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameChatRepository extends MongoRepository<GameChat, String> {
    
}
