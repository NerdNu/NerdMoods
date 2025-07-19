NerdMoods
=========
Provides personalised weather and time for each player client side.

Commands
--------
 * `/prain [off|on|reset]`
   * Turns rain (and snow) off or on in the the player's client.
   * `/prain reset` sets it back to server weather.

 * `/ptime [day|night|reset|<time>]`
   * Set the time of day shown in the player's client.
   * `<time>` is a number from 0 to 24000, inclusive and is the number of
     ticks (1/20th of a second) since 6 am.
   * `/ptime reset` sets it back to normal server time.


Configuration
-------------

 * `allow-personal-weather` (default: `true`) - Allow players to use the `/prain` command to turn client-side weather on or off.
 * `allow-personal-time` (default: `true`) - Allow players to use the `/ptime` command to set their client-side time of day.


Permissions
-----------

 * `nerdmoods.admin` - Permission to administer the plugin (run `/nerdmoods reload`).
