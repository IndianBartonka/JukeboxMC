package org.jukeboxmc.entity.projectile;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.jukeboxmc.entity.Entity;
import org.jukeboxmc.entity.EntityLiving;
import org.jukeboxmc.event.entity.EntityCollisionWithEntityEvent;
import org.jukeboxmc.event.entity.EntityDamageByEntityEvent;
import org.jukeboxmc.event.entity.EntityDamageEvent;
import org.jukeboxmc.event.entity.ProjectileHitEntityEvent;
import org.jukeboxmc.math.AxisAlignedBB;
import org.jukeboxmc.math.Location;
import org.jukeboxmc.math.Vector;
import org.jukeboxmc.player.AdventureSettings;
import org.jukeboxmc.player.Player;
import org.jukeboxmc.world.Dimension;
import org.jukeboxmc.world.Sound;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public abstract class EntityProjectile extends Entity {

    protected EntityLiving shooter;
    protected Entity hitEntity;
    public boolean hadCollision = false;

    @Override
    public void update( long currentTick ) {
        super.update( currentTick );

        if ( this.closed ) {
            return;
        }

        if ( !this.isCollided ) {
            this.velocity = this.velocity.subtract( 0, this.getGravity(), 0 );
        }

        Location location = this.location;
        Vector moveVector = new Vector( this.location.getX() + this.velocity.getX(), this.location.getY() + this.velocity.getY(), this.location.getZ() + this.velocity.getZ() );

        double savedDistance = 0.0D;
        Entity hitEntity = null;

        for ( Entity collidedEntity : this.getWorld().getNearbyEntities( this.boundingBox.addCoordinates( this.velocity.getX(), this.velocity.getY(), this.velocity.getZ() ).expand( 1, 1, 1 ), Dimension.OVERWORLD, this ) ) {
            if ( !collidedEntity.hasCollision() ) {
                continue;
            }

            if ( collidedEntity.equals( this.shooter ) ) {
                continue;
            }

            if ( collidedEntity instanceof Player otherPlayer ) {
                if ( otherPlayer.getAdventureSettings().get( AdventureSettings.Type.NO_CLIP) ) {
                    continue;
                }
            }

            AxisAlignedBB entityBB = collidedEntity.getBoundingBox().grow( 0.3f, 0.4f, 0.3f );
            Vector onLineVector = entityBB.calculateIntercept( location, moveVector );
            if ( onLineVector == null ) {
                continue;
            }

            EntityCollisionWithEntityEvent event = new EntityCollisionWithEntityEvent( collidedEntity, this );
            this.getWorld().getServer().getPluginManager().callEvent( event );

            if ( !event.isCancelled() ) {
                double currentDistance = location.distanceSquared( onLineVector );
                if ( currentDistance < savedDistance || savedDistance == 0.0 ) {
                    hitEntity = collidedEntity;
                    savedDistance = currentDistance;
                }
            }
        }

        if ( hitEntity != null ) {
            ProjectileHitEntityEvent projectileHitEntityEvent = new ProjectileHitEntityEvent( hitEntity, this );
            this.getWorld().getServer().getPluginManager().callEvent( projectileHitEntityEvent );

            if ( !projectileHitEntityEvent.isCancelled() ) {
                float damage = this.getDamage();

                EntityDamageByEntityEvent event = new EntityDamageByEntityEvent( hitEntity, this.shooter, damage, EntityDamageEvent.DamageSource.PROJECTILE );
                if ( hitEntity.damage( event ) ) {
                    this.applyCustomKnockback( hitEntity );
                    this.applyCustomDamageEffects( hitEntity );
                    if ( this instanceof EntityArrow ) {
                        if ( this.shooter instanceof Player player ) {
                            player.playSound( Sound.RANDOM_BOWHIT );
                        }
                    }
                }
                this.hitEntity = hitEntity;
                this.updateAbsoluteMovement();
                this.close();
                if ( this instanceof EntityFishingHook ) {
                    if ( this.shooter instanceof Player ) {
                        ( (Player) this.shooter ).setEntityFishingHook( null );
                    }
                }
                return;
            }
        }

        this.move( this.velocity );

        if ( this.isCollided && !this.hadCollision ) {
            this.hadCollision = true;
            this.velocity.setX( 0 );
            this.velocity.setY( 0 );
            this.velocity.setZ( 0 );
            if ( this instanceof EntityArrow ) {
                this.getWorld().playSound( this.location, SoundEvent.BOW_HIT );
            }
            this.updateAbsoluteMovement();
            return;
        } else if ( !this.isCollided && this.hadCollision ) {
            this.hadCollision = false;
        }

        if ( !this.hadCollision || Math.abs( this.velocity.getX() ) > 0.00001 || Math.abs( this.velocity.getY() ) > 0.00001 || Math.abs( this.velocity.getZ() ) > 0.00001 ) {
            double f = Math.sqrt( ( this.velocity.getX() * this.velocity.getX() ) + ( this.velocity.getZ() * this.velocity.getZ() ) );
            this.setYaw( (float) ( Math.atan2( this.velocity.getX(), this.velocity.getZ() ) * 180 / Math.PI ) );
            this.setPitch( (float) ( Math.atan2( this.velocity.getY(), f ) * 180 / Math.PI ) );
        }

        this.updateAbsoluteMovement();
    }

    @Override
    public boolean canCollideWith( Entity entity ) {
        return ( entity instanceof EntityLiving ) && !this.onGround;
    }

    public abstract float getDamage();

    protected void applyCustomDamageEffects( Entity hitEntity ) {

    }

    protected void applyCustomKnockback( Entity hitEntity ) {

    }

    public EntityLiving getShooter() {
        return this.shooter.isDead() ? null : this.shooter;
    }

    public void setShooter( EntityLiving shooter ) {
        this.shooter = shooter;
        this.metadata.setLong( EntityData.OWNER_EID, shooter.getEntityId() );
    }
}
