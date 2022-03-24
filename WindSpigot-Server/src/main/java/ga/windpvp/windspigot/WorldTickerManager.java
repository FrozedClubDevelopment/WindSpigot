package ga.windpvp.windspigot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import ga.windpvp.windspigot.async.AsyncUtil;
import ga.windpvp.windspigot.async.world.WorldTicker;
import ga.windpvp.windspigot.config.WindSpigotConfig;
import javafixes.concurrency.ReusableCountLatch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldServer;

public class WorldTickerManager {

	// List of cached world tickers
	private List<WorldTicker> worldTickers = new ArrayList<>();

	// Latch to wait for world tick completion
	public static volatile ReusableCountLatch latch = null;

	// Lock for ticking
	public final static Object lock = new Object();

	// Executor for world ticking
	private final Executor worldTickExecutor = Executors
			.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("WindSpigot Parallel World Thread").build());

	// Caches Runnables for less Object creation
	private void cacheWorlds(boolean isAsync) {
		// Only create new world tickers if needed
		if (this.worldTickers.size() != MinecraftServer.getServer().worlds.size()) {
			worldTickers.clear();
						
			// Create world tickers
			for (WorldServer world : MinecraftServer.getServer().worlds) {
				worldTickers.add(new WorldTicker(world, isAsync));
			}
			
			int amountOfWorldTickers = this.worldTickers.size();
			
			// Create latch to wait for world ticking to finish
			if (latch == null) {
				latch = new ReusableCountLatch(amountOfWorldTickers);
				return;
			}
			
			// Reuse the latch 
			if (latch.getCount() > amountOfWorldTickers) {
				while (latch.getCount() > amountOfWorldTickers) {
					// Decrease the thread count of the latch if it is too high
					latch.decrement();
				}
			} else if (latch.getCount() < amountOfWorldTickers) {
				while (latch.getCount() < amountOfWorldTickers) {
					// Increase the thread count of the latch if it is too low
					latch.increment();
				}
			}
		}
	}

	// Ticks all worlds
	public void tick() {
		if (!WindSpigotConfig.parallelWorld) {

			// Cache world tick runnables if not cached
			this.cacheWorlds(false);

			// Tick each world on one thread
			for (WorldTicker ticker : this.worldTickers) {
				ticker.run();
			}
		} else {
			// Cache world tick runnables if not cached
			this.cacheWorlds(true);

			// Only use multiple threads if there are multiple worlds
			if (this.worldTickers.size() != 1) {
				//latch = new CountDownLatch(worldTickers.size());

				// Tick each world on a reused thread 
				for (WorldTicker ticker : this.worldTickers) {
					AsyncUtil.run(ticker, this.worldTickExecutor);
				}

				try {
					// Wait for worlds to finish ticking
					latch.waitTillZero();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				// Tick only world on one thread
				this.worldTickers.get(0).run();
			}
		}
	}

	public Executor getExecutor() {
		return this.worldTickExecutor;
	}

}
