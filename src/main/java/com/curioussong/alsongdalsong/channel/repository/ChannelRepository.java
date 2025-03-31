package com.curioussong.alsongdalsong.channel.repository;

import com.curioussong.alsongdalsong.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {
}
