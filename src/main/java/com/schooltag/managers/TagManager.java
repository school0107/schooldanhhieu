// Thêm vào class TagManager

public void openTagMenu(Player player, int page) {
    try {
        this.currentPage = page;
        Inventory inv = Bukkit.createInventory(null, 54, 
            ColorUtils.colorize(Schooltag.getInstance().getConfigManager().getGuiTitle()));
        
        // Fill background
        ItemStack filler = new ItemStack(Material.valueOf(
            Schooltag.getInstance().getConfigManager().getFillerMaterial()));
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(ColorUtils.colorize(
            Schooltag.getInstance().getConfigManager().getFillerName()));
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Get all tags and sort by slot
        List<String> allTags = new ArrayList<>(Schooltag.getInstance().getConfigManager().getAllTags());
        Map<Integer, String> slotMap = new HashMap<>();
        
        for (String tagId : allTags) {
            int slot = Schooltag.getInstance().getConfigManager().getTagSlot(tagId);
            if (slot >= 0 && slot < 54) {
                slotMap.put(slot, tagId);
            }
        }
        
        // Add tags to their specific slots
        for (Map.Entry<Integer, String> entry : slotMap.entrySet()) {
            int slot = entry.getKey();
            String tagId = entry.getValue();
            
            boolean hasPerm = hasTagPermission(player, tagId);
            boolean isSelected = tagId.equals(getPlayerTag(player));
            
            ItemStack item = createTagItem(tagId, hasPerm, isSelected, player);
            inv.setItem(slot, item);
        }

        // Add control buttons
        ItemStack prev = new ItemStack(Material.valueOf(
            Schooltag.getInstance().getConfigManager().getPrevMaterial()));
        ItemMeta prevMeta = prev.getItemMeta();
        prevMeta.setDisplayName(ColorUtils.colorize(
            Schooltag.getInstance().getConfigManager().getPrevName()));
        prev.setItemMeta(prevMeta);
        inv.setItem(48, prev);

        ItemStack next = new ItemStack(Material.valueOf(
            Schooltag.getInstance().getConfigManager().getNextMaterial()));
        ItemMeta nextMeta = next.getItemMeta();
        nextMeta.setDisplayName(ColorUtils.colorize(
            Schooltag.getInstance().getConfigManager().getNextName()));
        next.setItemMeta(nextMeta);
        inv.setItem(50, next);

        ItemStack close = new ItemStack(Material.valueOf(
            Schooltag.getInstance().getConfigManager().getCloseMaterial()));
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ColorUtils.colorize(
            Schooltag.getInstance().getConfigManager().getCloseName()));
        close.setItemMeta(closeMeta);
        inv.setItem(49, close);

        player.openInventory(inv);
    } catch (Exception e) {
        e.printStackTrace();
        player.sendMessage(ColorUtils.colorize("&cCó lỗi xảy ra khi mở menu!"));
    }
}

// Cập nhật handleMenuClick
public void handleMenuClick(Player player, int slot) {
    try {
        // Check if clicked slot has a tag
        String clickedTag = null;
        for (String tagId : Schooltag.getInstance().getConfigManager().getAllTags()) {
            int tagSlot = Schooltag.getInstance().getConfigManager().getTagSlot(tagId);
            if (tagSlot == slot) {
                clickedTag = tagId;
                break;
            }
        }
        
        if (clickedTag != null) {
            selectTag(player, clickedTag);
            openTagMenu(player, currentPage);
        } else if (slot == 48) {
            // Previous page - not used in slot mode
        } else if (slot == 50) {
            // Next page - not used in slot mode
        } else if (slot == 49) {
            player.closeInventory();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}