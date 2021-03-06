/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 *                       Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view LICENCE file for details. 
 *
 * @author The Dragonet Team
 */
package org.dragonet.proxy.network.cache;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.github.steveice10.mc.protocol.data.MagicValues;

import org.dragonet.proxy.entity.EntityType;
import org.dragonet.proxy.network.UpstreamSession;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;

public final class EntityCache {
	// vars
	private final UpstreamSession upstream;
	// proxy eid -> entity
	private final Map<Long, CachedEntity> entities = Collections
			.synchronizedMap(new HashMap<>());
	// pro
	private final Set<Long> playerEntities = Collections.synchronizedSet(new HashSet<Long>());

	// 1 is for client
	private final AtomicLong nextClientEntityId = new AtomicLong(2L);
	private final Map<Long, Long> mapRemoteToClient = Collections.synchronizedMap(new HashMap<>());
	private final Map<Long, Long> mapClientToRemote= Collections.synchronizedMap(new HashMap<>());

	// constructor
	public EntityCache(UpstreamSession upstream) {
		this.upstream = upstream;
		reset(false);
	}

	// public
	public UpstreamSession getUpstream() {
		return upstream;
	}

	public Map<Long, CachedEntity> getEntities() {
		return entities;
	}

	public void reset(boolean clear) {
		if (clear) {
			entities.clear();
			mapRemoteToClient.clear();
			mapClientToRemote.clear();
		}
		CachedEntity clientEntity = new CachedEntity(1L, 1L, -1, null, null, true, null);
		entities.put(1L, clientEntity);
	}

	public CachedEntity getClientEntity() {
		return entities.get(1L);
	}

	public CachedEntity getByRemoteEID(long eid) {
            if (!mapRemoteToClient.containsKey(eid))
            {
                return null;
            }
		long proxyEid = mapRemoteToClient.get(eid);
		return entities.get(proxyEid);
	}

	public CachedEntity removeByRemoteEID(long eid) {
            if (!mapRemoteToClient.containsKey(eid))
            {
                return null;
            }
		long proxyEid = mapRemoteToClient.get(eid);
		CachedEntity e = entities.remove(proxyEid);
		if (e == null) {
			return null;
		}
		mapClientToRemote.remove(proxyEid);
		playerEntities.remove(e.proxyEid);
		return e;
	}

	/**
	 * Cache a new entity by its spawn packet.
	 *
	 * @param packet
	 * @return Returns null if that entity isn't supported on MCPE yet.
	 */
	public CachedEntity newEntity(ServerSpawnMobPacket packet) {
		EntityType peType = EntityType.convertToPE(packet.getType());
		if (peType == null) {
			return null; // Not supported
		}

		CachedEntity e = new CachedEntity(packet.getEntityId(), nextClientEntityId.getAndIncrement(), MagicValues.value(Integer.class, packet.getType()),
				peType, null, false, null);
		e.x = packet.getX();
		e.y = packet.getY();
		e.z = packet.getZ();
		e.motionX = packet.getMotionX();
		e.motionY = packet.getMotionY();
		e.motionZ = packet.getMotionZ();
		e.yaw = packet.getYaw();
		e.pitch = packet.getPitch();
		e.pcMeta = packet.getMetadata();
		e.spawned = true;
		entities.put(e.proxyEid, e);
		mapClientToRemote.put(e.proxyEid, e.eid);
		mapRemoteToClient.put(e.eid, e.proxyEid);
		return e;
	}

	public CachedEntity newPlayer(ServerSpawnPlayerPacket packet) {
		CachedEntity e = new CachedEntity(packet.getEntityId(), nextClientEntityId.getAndIncrement(), -1, EntityType.PLAYER, null, true, packet.getUUID());
		e.x = packet.getX();
		e.y = packet.getY();
		e.z = packet.getZ();
		e.yaw = packet.getYaw();
		e.pitch = packet.getPitch();
		e.pcMeta = packet.getMetadata();
		e.spawned = true;
		entities.put(e.proxyEid, e);
		mapClientToRemote.put(e.proxyEid, e.eid);
		mapRemoteToClient.put(e.eid, e.proxyEid);
		playerEntities.add(e.proxyEid);
		return e;
	}

	public CachedEntity newObject(ServerSpawnObjectPacket packet) {
		CachedEntity e = new CachedEntity(packet.getEntityId(), nextClientEntityId.getAndIncrement(), -1, EntityType.ITEM, packet.getType(), false, null);
		e.x = packet.getX();
		e.y = packet.getY();
		e.z = packet.getZ();
		e.motionX = packet.getMotionX();
		e.motionY = packet.getMotionY();
		e.motionZ = packet.getMotionZ();
		e.yaw = packet.getYaw();
		e.pitch = packet.getPitch();
		e.spawned = false; // Server will update its data then we can send it.
		entities.put(e.proxyEid, e);
		mapClientToRemote.put(e.proxyEid, e.eid);
		mapRemoteToClient.put(e.eid, e.proxyEid);
		return e;
	}

	public boolean isRemoteEIDPlayerEntity(long eid) {
		long proxyEid = mapRemoteToClient.get(eid);
		return playerEntities.contains(proxyEid);
	}

	public void onTick() {
		// Disabled this for now
		/*
		 * entities.values().stream().map((e) -> { e.x += e.motionX; e.y += e.motionY;
		 * e.z += e.motionZ; return e; });
		 */
	}

	// private

}
