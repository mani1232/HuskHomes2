package net.william278.huskhomes.list;

import de.themoep.minedown.MineDown;
import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.player.User;
import net.william278.huskhomes.position.Home;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class PrivateHomeList extends ChatList {

    private final HuskHomes plugin;
    private final User homeOwner;

    public PrivateHomeList(@NotNull List<Home> homes, @NotNull User homeOwner, @NotNull HuskHomes implementor) {
        super(homes.stream().map(position -> new ListItem(position.meta.name, position.owner.username,
                        position.meta.description)).collect(Collectors.toList()),
                10, "homelist", implementor.getLocales()); //todo config settable items per page
        this.plugin = implementor;
        this.homeOwner = homeOwner;
    }

    @Override
    protected String getItemDisplayLocale(@NotNull ListItem item) {
        return item.getFormattedItem("home_list_item", plugin.getLocales());
    }

    @Override
    protected String getItemSeparator() {
        return plugin.getLocales().getRawLocale("list_item_divider").orElse(" ");
    }

    @Override
    protected MineDown getHeader(int pageItemStart, int pageItemEnd, int totalItemCount) {
        return plugin.getLocales().getLocale("private_home_list_page_top", homeOwner.username,
                        Integer.toString(pageItemStart), Integer.toString(pageItemEnd), Integer.toString(totalItemCount))
                .orElseGet(() -> new MineDown(""));
    }

}