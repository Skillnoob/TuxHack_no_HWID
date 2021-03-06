package me.tux.tuxhack.module.modules.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import me.tux.tuxhack.module.Module;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OldNames extends Module {
    public OldNames() {
        super("OldNames", "pulls up old names", Category.MISC);
    }

    public void sendLog(String msg) {
        mc.player.sendMessage(new TextComponentString("§4[OldNames]§b "+msg));
    }

    public void sendBook(String data, String title) {
        NBTTagList nbtPages = new NBTTagList();
        String[] pages = data.split("\f");

        for (String contents : pages) {
            String str = ITextComponent.Serializer.componentToJson(new TextComponentString(contents));
            nbtPages.appendTag(new NBTTagString(str));
        }

        NBTTagCompound nbtBook = new NBTTagCompound();
        nbtBook.setTag("pages",nbtPages);
        nbtBook.setString("title",title);
        nbtBook.setString("author","TUXISBASED");
        nbtBook.setInteger("generation",1);

        NBTTagCompound nbtStack = new NBTTagCompound();
        nbtStack.setByte("Count", (byte) 1);
        nbtStack.setShort("Damage", (short) 0);

        nbtStack.setString("id","minecraft:written_book");
        nbtStack.setTag("tag",nbtBook);

        ItemStack bookStack = new ItemStack(nbtStack);

        mc.displayGuiScreen(new GuiScreenBook(mc.player, bookStack, false));
    }

    public static List<String> getOldNamesFor(UUID id) throws IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL("https://api.mojang.com/user/profiles/"+id.toString().replace("-","")+"/names");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();

        List<String> ret = new ArrayList<>();

        try {
            JsonArray jsonArray = new JsonParser().parse(result.toString()).getAsJsonArray();
            jsonArray.forEach(elem -> {
                ret.add(elem.getAsJsonObject().get("name").getAsString());
            });
        } catch (Exception e) {

            ret.add("ERROR BAD RESPONSE");
        }

        return ret;
    }

    @Override
    protected void onEnable() {
        if (mc.player==null) disable();
        try {
            mc.player.closeScreen();

            List<EntityPlayer> players = new ArrayList<>(mc.world.playerEntities);

            new Thread(()-> {
                try {
                    sendLog("Oldnames are being looked up, you will be notified when finished.");

                    String oldNames = "";
                    int count = 1;
                    for (EntityPlayer player : players) {
                        oldNames += " " + player.getDisplayName().getUnformattedText() + "\n";
                        int lc = 0;
                        for (String name : getOldNamesFor(player.getUniqueID())) {
                            if (name != player.getDisplayName().getUnformattedText() && lc<=4) {
                                oldNames += "   - " + name + "\n";
                                lc++;
                            }

                            if (lc>4) {
                                oldNames += "   [truncated]\n\f";
                                break;
                            }
                        }
                        oldNames += "\n" + (count % 3 == 0 ? "\f" : "");
                        mc.player.sendMessage(new TextComponentString("Finished lookup for " + player.getDisplayName().getUnformattedText()));
                        count++;
                    }

                    oldNames += "\freport generated by TuxHackTM\n";

                    sendBook(oldNames, "Old names");

                    disable();
                } catch (Exception e) {
                    sendLog("error");
                }
            }).start();
        } catch (Exception e) {
            disable();
        }
    }
}