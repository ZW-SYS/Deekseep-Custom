package com.dsmod.probe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

/** YunSeek whale icon embedded for host-process UI (module resources are not loaded there). */
final class YunSeekIcon {
    private YunSeekIcon() {}

    private static final String MODULE_PACKAGE = "com.dsmod.probe";
    private static volatile Bitmap cached;

    static Drawable get(Context c) {
        try {
            return c.getPackageManager().getApplicationIcon(MODULE_PACKAGE);
        } catch (Throwable ignored) {}
        try {
            Bitmap b = cached;
            if (b == null) {
                byte[] raw = Base64.decode(PNG, Base64.DEFAULT);
                b = BitmapFactory.decodeByteArray(raw, 0, raw.length);
                cached = b;
            }
            return b == null ? null : new BitmapDrawable(c.getResources(), b);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final String PNG = ""
            + "iVBORw0KGgoAAAANSUhEUgAAAGAAAABgCAYAAADimHc4AAAjsUlEQVR4nNV9e5geRZnvr6q7v+vMZJJJCAIhBIgLQhDCRQQENKyo"
            + "BxHFwcVFdxXxxnnUdY/owWVjxCiu54i7io/n8bq7rquOely8IMoieEXFgGICCCSSCJLLZCZz+W7dXe/5o7uq3qrub2Zy2V1PP883"
            + "X3d11Vtv/d5rVXV/I7Afx+iXKRi7QqYAAQCufT+N9ILkXEE4HyTWEtGxIFoKoAEIodsRSDcxJfqL/LLSKwBUKHEI+OSJl+hqvJws"
            + "X8TPQVlXupyyFqSICKIFhT0ksBUkNynQD6iLH3/9lkXjGVGB0VEVjI2JtA+EhUPMXwVYv54kAGzYIBQAXPOB+CIp5VVEdJGUwZEy"
            + "AEgBKgVIKRCpEtAtTBbLIqhFOfl1inSL1Sz8RG4byguJk9ECUFYARUEg16kAQgYAAqikB5WmT5CQd6SUfv7WjwzfofHagPcAGzao"
            + "wgC9Y14BZFqfSfSaD8aXBpDvkoF8tgyAuAukSUwCUCQgQCS45rMhLxx0t7KtUQZ6+YkB3ZUJGbKONeRlivI6rC4XBOn7+gCIFEkZ"
            + "VIUMG1BpjDSNf0pxctOttyy5FQBGR2lea5hTABesp/DuDSJ53Y3tVUEUfTiIgsuggF4vViLjSwoBUfQO/3Wg+/WJ09WCIduGuyFT"
            + "pt0O6XvWGpQp14IhIiJFBBFGA5IApEnn65321Nu/96ljtl1wwffDu+9+blIAID/6CkBr/us2xi8PQvmJKJIj3VacEkgIIWSZ2Vsg"
            + "imCUlhws6PyatyPmgjy6BnRdrN0MrFsild3UwlOOBbBzEJRidEkpIqKoOhwkcXs8Ub03fufjy78ylyWUCIDE6Jchx64Q6es39q4P"
            + "q9HGNCakaZJAIJw7mB4g6F7FBYPu0XWVggxfrmy4MGwbCyqsIBxNZ214rCCreMZ6lEpEEIVBUEPcm3n3bZ84/P2ZEKAA4TAv/XGs"
            + "X49g7AqRXrMxvqnaiDbG3ThN0lgBGnwbmay5usMkeAO33Lk1zEhZKRVOUDxz6VIBfEvaZYoLhViZR5183vI+lA3QhigD3vQoRKiS"
            + "nur1ptOounjjxW948qaxMZFesB6Bj7cjgPXrKdywQSSvvbFzfbURvrM120uISAoIadg2vPm6NR/omuFy0IkNthx0KqXLdcJt7QJP"
            + "vNSXI6y2W/dlBUt+Q0IuDKZ8THGICBBCQpHstceTanXJOy9+3Y7r794gkgvWfz/kmBkXpH3+1e/tvKxar3611+slUBRACOEy4B6F"
            + "koX69YJ6+jY0F81iMffrpoQX6T8cK2UrkLZqIHMx3FKM5sMEZCcmMMUEyNDN75GATGXYDHvd8cvv+PSqr/GYIIEsbx0bhXrTjbQi"
            + "COWn4yRVpEgShLCm5cIzt7az63k03er43Jrud07kXRhVJQOIQ8eARFbbjeiyMmVIMboKDGRP07Vd8cFxwWUXQqlUpklHBUH9089/"
            + "0/iKsTEoPbeSALBlCwSEoC71bgmjaDhNYpXdOwSgW1xKQXdc2QJAd9owIIwVOR3yvN5VImgX43eXlymfP7J0iNG1tFmPWhB6zAJS"
            + "pV0VhM1h1Z29BRC0ZcuYAAChXc817+u8MKhUvx13OimECDRBFwi/hAqVqP9JwdH0p2urFdowum4XXurJ/A9XGqMfZFtQnkoqx1Ly"
            + "OiYOkBEuj4MmADPA7fIF4zWrn4bRYNDrzrzozs+uuG10lAKhTWFH0Pl5VKmtjbsdpQXgA1gcUQGTQptDArq59Ga3jBfHyDyBmSwp"
            + "79cIwCg2cx05uiY8EEzANdd5PcfXO3StIO39XABhQ8a96U13rvzcWcB7siD82g3t50bV2p29XkfZjKcEjBLM+oNeBmIZAXgyLbYp"
            + "gF7Szk8UeAwotwBPEE7GY1NOTcu4HlVSRpaOtgBQvrzBBAEApJQKoqZMujPPu/OfVn1fAgAJ8RoZgGCmGFqjuJp4GBhuTU03gMHh"
            + "vgg+Y7qsjQ6W5DdgXbvgl/l0qzjk9WXdiZ+iKodXguUFTj88KNsYo5QO6B5odjxKiJAg8RoAEG/6wOTiTrfyYBBWlqdpl4qLae5Y"
            + "Crrqm7xhey4aLjj925T15fWjBVlC17gUfTD14i6GW0b57BcgUo4ucTdjr7kywFgZVwhSREJWhEq7O+NmcGLY7kXnhVF1eRJ3SAgN"
            + "vh/gynA5FKCXtZoP9LwVNxgPdPPXNRGoQluygvLciyaouKXqQGzIutfGGpjbcZY+so+gtEMyqC2vTHfOCwXhQiklBEgRIYDoM+gy"
            + "NAggUVJ5HtCLsO8H6F4lm+x4FJVbqjWSt1daa51Ve9fXw4DMgCwAzy2GHNC5UjBrVUKGgZJ0YUjAWqUISvse122ZAu6Xiku8/iVB"
            + "FO4fJOiWIdaG67slxMcPDiZsYNTqyF2GHz+4CzLOS1sPi1GugKxVmTF7lggiQSoFEdaGIKxSaQKhICAKsLCxl7kK4dUsdwUcHJ9C"
            + "8egPuiXDqDNwTN+8L84Ld1u5VHyQs3OW33M2/OwGtoy1tnLgNB3eSSgVg0CrQhCWKpWCAAEiX9ULkAmvs+JxkKBTsSYH3Z6WcMBc"
            + "g2P25rYGuFzTlbcPoNsU/b91MzqzMu6ljDdHmAQAglQCUlgaElTDpAR6EB44BUz9C1GclRaqFZB3kCpQ90HPLkvs0xMMefxarWZ1"
            + "+mQwpg8nK7JlFmBXmLYb5eJQyr8uUxCCGiEgRCno/a5KNLS/1sI7fHBsJSqjm98gv3aplpVkKKxuqgBShEAKps0lky1uRUwKRtNZ"
            + "uRM/9Bg8wWiFcNyiLgNEqDMB4d5wwRA2OHA4hIdyiVPgOPowFgQnBMwWX39hesriAa3P+YCTRKFZF6hXBZ4aTxGFwvLkaLur6bpM"
            + "92gDsBWG414cF+dapibKR0UESG2HejlWQeexLLUiPVt07NLRiOJqo6nGwGDmzWkiWwhrdbw1SNY/L3d48DXX54UUpmZinHtqBde8"
            + "fBAzsz3Y2W6+BA3t05kwWFm+Ew9QlrryJW3risj5WJDZR2PArCLsa/qshBQgfI0UVuL6EORTsZpesCp9KghxoiCEwpknVXH/wzEk"
            + "m4wX2SvXMuNODKCZ5gcBIZQx1p3dwOqVNRy9HNi5N0YUhQABUgooxSwgHy/lnbiant9kY+ICsIwx7HiQYF5EF0ty7c2VolXBfH2c"
            + "mamyhHV9xdswC/Ilb/qBQpqmaLe7+Ks/H8JlF9YzDWUWwE+0litV1DKlrFUpRZBC4ejDJSYnOzjr5CqOP7oGALj0eYOYnu5g2TAw"
            + "2AR6cZoDopg7YTgozm9R2zWg3Ivo0gKuJV5EgggK+YfhxDyGY27cPPmKodFDytfVBUEKPRhkdZUFEUQQpDA93cHrLx/Es06pY+d4"
            + "DyqNHRfhg94PeC7cVjvGisMFrrt6BL1eG+ed3gRERuOCM4cQx11c9eIhvOC8BianepBCWW0n9pAWMf7J5Z+DbhcNOWgZDi7o8HAE"
            + "JCnwB18yDUKmzdbf+dYBQ5hyji0jCiCF6ekYk1Mxut0UApTHcVtPgDC5r4t1Z1VxyYWLoAhodxIkcTIH6Fb7HOC1MCjT5CSOse5Z"
            + "DRy1PMJz1jZw50/2QCBzN3fdM47lS4BzThvAurMHsGSI0OulRkms9ep+GAZQzDMw69ZtmBX6mm4RY16ECKGWnTV1sAVR5m/zkkIs"
            + "YD5SCmBmNsWLnlPFqScM4N7Nbfz6t108sbOHMAzQrAdQIqPX6SUYHkjxmpeNQOVtJ6diKKUcc9egaz54z9zf64IkVahXCc9a0wAA"
            + "XHTuMN7+/ofR66UYHAjw7bvH8ZJ1h6FakTjisAqOPSrErx+OMdCUVg9RFDq01pr7TKMdHrQROOjArQBzLywuMVjECd4CEdkVxbI1"
            + "awWClAq/3DyDN4wO4Vlramh1FO7d3MY375rGbx7toF6LUK8JTMx08LIXD2LxUIg4JshIYNd4z8jeX951BgfLH/g5ZblBu5Pin2/d"
            + "hfPPGMLXbt+DJYsa+OWDXaQpYXCggV88MIsf3juJqZkED29toVqpIlUKRDo91fFAY2xdEQAbpOEqCgzomh9zZjFS7rV4xXV7uToV"
            + "n5Vzin1hkVtCBIEUe8Zn8GcvGsTrXr4ciggym+vhez+dxj/fOoHdEykG6yluuWEllo1EUPkE6d03b8UvNqcYXtQE8UeWyPZkBFIw"
            + "3NzvKgWV9jAz2wGpFFJK1GoVSJnRS1OFbi9Gt9MDQaBeqyKMKhD5RqC7wMbAd/aNtSC0a3EVnFiCYlDS4YGXE3wLYMGiKIfCmXud"
            + "iShVwOBAhLHbduHsU5o4+ekDiBNCFAo8/5xBnHZCDes/ugONmsBhDPw0JTy1O0YUhkzrjFzhIG6Ad3XHutAQA81apsmQIEhrs0Kh"
            + "WpGoRCGgH+gGnx1rl2P7VcQ71XUU03Tbt62qPKissnKXKZ2JAn8GhknWnWSUXecMUSaIMAghZQXv+/g2jE/0EIUZwKkiLFsS4UPX"
            + "HY2rX77cEePuiR52T8QIwyAPsLYfOP3kwTbP90h5vAAQQkCIEEJGEDKAlBLZXpPINB0S2ZOWAZA/f2D6UToTyhIKZf2QtQYT98h+"
            + "wHExEwnz4WPi5iCJDxZMEF7a6ALfTzCZVihINJo17J4UeOeHfovJqRhBIIxWNeshTjxuwBHA9ic6mJ6lvJ7bj8nGdNqrMxSyWiUF"
            + "IAQBpBAnCp2eQqujMNtK0Won6HRTJEkKIgUpBWQgMqEYIbK00bidXCiw+X3Gh05bixg4voLKQecz49DJUQvuhW8VuZkRNy3/EEJA"
            + "qRCLhpp4dPsU3nLjFrz3batxzJENrjBZapqfb3lsBmkqICDspolH3mYWGTdSAGmi0O6l6PYycGsVoNmQGKhL1KsCYSiRpoTZlsLU"
            + "bIrpWYVOlyCEQK0aoBLJDPR8psmzIBNavIzIxCNVwhtzL3DqUREpIoQmYDj4loFMfqWSOuzIhTC8eBA7ds7gzeu34NpXHoX/9rzD"
            + "QWQzXb0N/fDWFoJAQjFSBgxWJkU2olYnRaeboFkDVh0R4aTjB3DCcTUcc2QVyxZHaDYkKpEN5L1YYaaVYueeGI8+3sF9W2Zx34Mt"
            + "PLGziygM0KyHxdk+A9RYhnHkliligjM381MetDlBXSpe+tZdRVWb81owUZBzm5POtpYVVJoClKDTaWFiYhoXnTOE977tRASBhBBA"
            + "mgfhr96+Czf/4y4ctnQIiqTVvpymzGeyM7MxiFIcf3SE888cwDmnDuLYFVUjyP059s0k+PEvp/DV28fxq4faaNQiVCKJ1GQ8lgl3"
            + "Bs413bUQMAvJL53KzhkB4rK37HRRLByexBzQRS6JopR5R0plQkh6HczMTOELNz8TRx1edwbR7Sm8/oZH8dReiUa9apalpcxmo1Mz"
            + "MaJA4cyT67j0ecM4c81AFi/yI1Va8AJ5vHU39/SYKdNmAYE8MwUR4Zt3juOWf3kK45MKQ4MVJOylIuvDi9ruZE4cKZ5VcS4ctAmh"
            + "ItX3PSVif7InSbWJ5gHPiR8lnWVNICBBCBFGVSSqgvsfnMaRh9ezFDQQUIpQq0r896sOx//44A5UKwGkyKxg31QMIVKcf3oDr3jh"
            + "CE5a3XBAF0JACiCQc1uA5l8Ic2UCr5QCL163FKevGcTffuR3+OVv2li8qIokLboWCzjz6ua2BcPOt4qg8zbSaEWeVSilP7BrIzlB"
            + "fQ9wMyW9bqSzFZ422kwle8Uziiq4/YfjjtClFEgV4axThvDay5di5+5ZTM10sW+qjbXPiPC/33kU3vuWFThpdSPnI2sXyAz8Az1E"
            + "LjgBIE0JRxxWxS3vWY1z19axd7KTK5mNzmath7sZs36kd1LgrQpr2Pn6kJYAQVxy7ZP+dLY/w9DE/KNPG3JPFClA9TCxdxIb/3oV"
            + "nnv2UsQJGRCJgDAU+NTYU/jFA7N41UuW4bzTh8w4QTBu41AdWkkCmVmilAKtdoq/vG4Ltu5I0GxESJVwMyHd0Il7VrOd5KEEB+5Z"
            + "pCNVo7nkfHjebRcM3dzcndBRcYJkPFaAeqOOmz6xDfdtnkQUCgRB9glDgUcfn8WTO2dx4VkNnHf6ENI0s0YpDj34gLUCwFpiox5g"
            + "w9tWQSA2cwctLX9FmL9OY+cmZMarT3QZcTDgLEX4WtwncvOHh/gzKvktrglldIkEKlEFrXaCt974IC5+zhKsefogOj2Fn/1qAr/4"
            + "9QxaXYEkSVEJCJe/8Ijc7R2Eryk5dCo8PtHFg49M4Nwzl0MIYZZFTjxuAC+5aAT/+s09GBmuI2ELcHZ6pMEF03x3zGVlWXnumF/4"
            + "ht97yJcIwndRZdMB55FGrzOHE8p/ziBGHPcwNdVCmmYpRxhmaziVKMDMbBcji1J86aNnoFbNlgsOINPse1Ae01711h/gO3c/ife9"
            + "4zS8+dUnIE2zvQshMmu88i0PIKrWwN5T1KMAx6a4dlWCgz8nQL4jlq3j6I/vSpjL0WakAzC8up7L0UHddVMayRBhWMPIyCIsW7oY"
            + "y5YuxuLhRQijGlKK0GzWsf0PCb5xxx/ypyX6x6aDOaIoQhRVEEX2nRSZu6TVxzTxjNV1zLZ6kHqs0Gs9LMCSXpbQIFtJOa48B10D"
            + "RASEit9gX6VHYVbnN2BBBtYjlZIUEtkqpMwe8DVeRkAgizXNZh2f+9rv8YILl2OgGRm3caiOIBD4h/eeiUe2TeGMU0ZMGQCTIp96"
            + "4gB+9qsZDDYrAGkz1+DagOsczjzIlrk1szO7Guq9DchX9oiUCcCK/Pss+Jj4QoBOZ30LUypLXdmrn3q2ABJMmwTqtQp+/1SCT/zL"
            + "NjMTPlSHFuSiwciAX3Ycv7IB5Lt05qEdlnQwH8RSVAYyK7Nui0yAlo57MS7HEstAh/Y7XkZkhZAlBHYOoZzMyLottx3vh5XnQKdK"
            + "YvFwE1+49Un87P5xBEEWIA/loWOBX5YSIU0JI4srCAIyW6XcfcwLOpWDzmYFkAZ05QGm/Rnr0H8oycQPpbKdKIIHbPEaPuO54OD1"
            + "nWEiIGSESqWOv/3wQxif7JqZ80IPpTIg+320Jer9CiCzjkooEQQCg80QQr8srAXWB3SNkwFd+1/tWVjGpOuHsMXMxzFtYNf2Lnm+"
            + "ncwkzf61vKHsHvn9WLqcKpFAo1HDk7v34R0bf43/84HTEQbSbHXOd8gDmCrv3N3GfZvHcfc9u/C7J3po1KNcOF68K4RN8r6cwZTV"
            + "RKgcwnNGYCM4f6MesJv183WYXfQXsskszLlAqiSWDA/gp/dP4q/fdz9uvuE0YwlzAZwqwv/8wM/R6SocfeQAhoeqWDQYoVrJZnRK"
            + "Aa12gol9PfxhVwtP7sw+U9MxGvUIzz59OYYGqphtz2BgIJ8NHwLQ+fjF8/7iMVvNz3K8xsW3XooW4rFQStPjy1zYquQ0I1JQKoVE"
            + "jF27J/H88xbjQ+8+FbVqgDQlZ1WUH6kifOVb2/DgI/uwd7KHqZkYrVaMXpxtnwZSoFKRGGiGWDZSw5GHN7F61RD+5LhFOOaobMfu"
            + "imvvxeZHOmg2qlDkTYA4vwVs5rZ4PUZx4asfJRBP74ruZc6jjImSDqmE6WJVV3scL0sKlKYQIsbuPftw2jMa+Lvrn4mjj2iamHAg"
            + "7mau4yOffgT/8LnHsXRkEIr02nVhRKx8YaDzw+x8zJnB+MFUUZ5O2vTSfy7UhB0TtGHKNSMm0LOAxVcbTb96Q1ZIpCrA0pEh/Prh"
            + "Nq689if4+ne2Z3u8+WJa6j0umaaEhAVc5Y0pVYQ4Vuj2UvR6do/xc2OP4eP/9BiGF1WzzCvnv18wtS6aY6BBZ0mIEVFWR1xw1SO+"
            + "0yr3Z36dPlImL5CWV/XaODQ4D36mASB3R0KkaLfbmJmewfnPGsG1r16NtWtsPq+zG70xY15xIPsNoOC+tj4+jb//zEP4xh1PYdHQ"
            + "IGRQyZ6csG/wOvz3c9393KyFJ/srzv/z35IoNvXaeaZVQLgM9KJf92kWm5T0Q86tXMCZBQIJQAkmJmcQBgnOO2MJLn/hCjz7jMMw"
            + "2Ix86qVHp5vgNw9NYOv2aWx6YC++c/dTmGkJDA8PQFEIiAACsjj6AuhlyueDXsRBnP/Kh4ukuPiEd+12V95hCQP2aw5BcL/vVCsf"
            + "WBacFaRIkCQxpqZaUGmMFU+r4tSThnHqSYtx/MpBHLa0hkY9+6GqVjvF3skO/rCrjYcf3Yev374dv906DUAijCIMDtahKEKtVkGz"
            + "UQGRMHOoMtCLfn1+0PmYxXNe+ZCHS1lE9zoEXxz2Nb3YYdGqCqIod2UeX94YoP0riKAohYACqQTtThftdhdpkiAMCbWKRBRlO1/d"
            + "WCGOFdodhZnZBGevXYpLn78Cp5wwjGVLa9i6vYUdT3bwzX/fjc2PzGJoMHts0XeT/ca7vxYfzmX2DhimTBhipUz0s5gy0L2+Fg46"
            + "J5upghABiCQgBOr1AI16HUCKNM1n6flqbaUiIAOFZpPwd3/zJ3jFJStM9vSlb2zHp7+4DZesOxJf/Nhp+Pz//T0+8pnfIQpDBGFQ"
            + "MgM/cDerq4RUcsM0LbGA0oDZr43XmTnrY7L8hHhVn7dSHrLZOOWPHmZZWQAhCDIgSEmAANI0hZAJbnnfKTjn9BGzVDHTivGBjz2A"
            + "1135dHzyXx/FunOX4ZorV2LF02r4qxs3oyarcDaFyOGUsTI/6BwD85KebuqkjaYyKwf7EJz0FIRsSpyv4Dmppl7g8bdAvXUovelu"
            + "fiTJRF/dRjEeFCu3CpCxmT90K2SexWTPiE7PxLjuDcfhnNNHEMfKPCkx0Iiw7tzluPmTW3DCcU2sOKKBOFF4wYXL8eZXrcTkvjak"
            + "tOOx6z2wODju2ww+r8nX0Cy2kj8J4Wy++AMzoJPJ/dmbY6wz854lYyIHEExQfecLWop+X1Tsy4DOhOmDkfclBNBqxTjx+Ab+7NKj"
            + "oBQhDGX228siS0ffcNXTsW+6hytfcgyGBrMsSinC1a9YiZVHVNBux/mTEpauFUYf0MEwsGpusJWu6fPJlycEvbMFlwHOBJmfGLRA"
            + "mjfQHHAVYyI3AabV5A/MsUJ4S9c+6GUCzp5jmm33cNG5I9liXj7nmmnF6HSzXxVOEkIYZO8QADDvNTTqIS48ewSzrS6k4ONdAOjG"
            + "LVn+NV8AZb+MyEFnrRzXY+44zwLZpVndITc3S87SdVwS85aF2aIB2JudKzuw+UAn2L5IEaRIceLxg5lm51bxwY9twrf//XEAQKqy"
            + "SR7nS2N84uoBZL9wwi37wEC3Rlz2ipJxOR4HRbacAvMrCY5FsTNyywzIfWh6rT0TLhD2qnj9ILM0CUKznu39RpHErj1tfPHftmLz"
            + "b/fhshccmy9vF1+/EgJoNrJ9gQwv/YAmeUOikmFyJSvcRGjplINeGCKrRiXgOGclHRYE/h8Kur0SAkhShYl9PRBl11+77XfYN0OY"
            + "mSX04hSDAxFIAXFStICJyS5SpQp0DwR0rnhy7iACZlraxfjBFLxmuYthrsxh0gRS5jRYkGYOxnFlvnthfss1ezYKIAuo923emwkj"
            + "UbjysuNwxjMPQzcGZmYTfOeuJ9CNUxxzVNMITWdJmx7Ya2KC6Ym7WY5YjgFxDDgODAPJgT8w0Pv4dS9+uKBr4BcIOgd+PtA5GKZq"
            + "luU1mxXcfteTmJ6NIYTAosEK3vHGk/HItimcdcltuG7jL3HlS47FGc9cZshLIbBrTxvf/8lTaDYr+Woruf0sBHSDA1Ny8p+MKzMt"
            + "cHpk/85jWpyOS3oO91JCl3K6RapFft1huG2IgFo1xPYnZ/H3n9qCv3nrM9GLFZ57zuH41j+uw63f3YFjVw7gFZeuyjb/83lMKIEP"
            + "fvwB7JmIMbKk7ry9U+peSnFweeV3Q39w/fz6gYDutXboUqFsftC1ppUQKwqY0dUlSgksHm7ik194DCue1sBfXLEaAHDayUtw2slL"
            + "8jpkXhoBgI9+5jf40q2PY/HiIaRKv1Z1cKBzbEPnZlHl+0r5QILpvKDbDh26BwM6pykgQCLAwEADN/yvX+GhxyZx7V+eiKOeZl8Y"
            + "1OtC27ZP4eZP/gZf/fYODA8P6V/yd8ddBnrhaw6LJ0Cc9dL7irD9sYLuVF0Y6H6P2RuOKUAx9k7MYOmSAOecvgwnn7AYQwMRJqe6"
            + "+NWWvfjJvbsxOa2wZHgQhDB7vVXI/bP4Mi/i4SDOeukm5l0WCLrbZ2mHRZx8JuYDvayveUD36Jo2njCzoJhCihTdbg+zsx0k7J2k"
            + "IAgwMFBDpVKBUhnwwn05y6FbDrrHX6mbzYIwwX/2+xAH0z8G0F2WRP4fRgSiSGLx4ir8V3KVyj46F/XpHgjoGavkXIVE1BJCNkGq"
            + "OLCDAN1e/rGA7vGmN2JFkINi38zUj6qazQ1nJeBgQdf3JYhUKwSwB0I2iVIyW9dzgu7BsVDQS5jWX+WgF3oqB72Ebn/Qi2W2RAuE"
            + "M0UL421BoOdtcoCEkAKk9oQgtU2IcCVRj2DeeSl2eCBp4/y5ellfCwumCwXdkvD4O8C0cU7QPX45XY93gpSCoLaFIGwC5IWKQML9"
            + "WdwDAr3oYv7IQS98+VZ1KEB36RIRZa/u0iaZEu5SqgcByl9ZZYsBlBP3lgKcnSrNJFtiyBtCC8NZdmB0++4i2dZgBPKSfmswuqq3"
            + "HDDHkggYb8TK7Hi9JREmorlxcGpCLzuwnTRJKgaUvCtsUvdHrYR2iiBaTqpHIPYrrVze+xFMF5yre3QN03NqunvRT9OLpZZuP00v"
            + "Xh6cxTPuOW8kRCiU6u7sCfEj+aNvPWcCAt8NghoRIXV0tWzhy9F03ZMr5QVrOum+mDZxTdeDK7WqoqbbvrgwGW/zaDodrMUbq2Je"
            + "xNzO6iuiVAQ1AuG72390yYQEAJHSZ5WKBYhkEXQL0Fyg82G7TCwcdAOG1YC5QQcXcBF0lILOBVwE3eK1v6Bzqox/jgMRBEiSSgRR"
            + "+lkAEMB6ifXAqfc+/+dB0FybJi0lhAhMDGAMmPHxa+f2fMHUN3vWxrV9r3nBjL0Sl9fyvg4smJa6F6dLv2OvlJzxpDKsSZW0Nj32"
            + "k01nAYAcHX2PwIYNCkLeoP9vZGG9ew5NL7iYMk3vp4EmmPqaDteVgfVjwPc1vawv35WVa7qr7X00HUzTOQ4lmu57EZc3IUiIG4AN"
            + "CqMnCTk2JtLR0S8H93/z2bclvelvhJWBgIgSx+z/g0HnA5sbdA58f9BZqVM2F+i2L67O+wE6x8GK0vajVBKEzSBNZr6x9ceX3obR"
            + "LwcYuyKVADD2jFECSESVyrVp3JoUIpSk7K9jzgc6l/LcoHONKhnYvKCT62v7gO5r+0JA56Owfn0/QDc4EOtLd0UKMpIqaU+KkK4F"
            + "SGBsMwH6/wlvEGp0dEze+29n7VBp52oZVCQJoUBEC8nVSzOYUtCZVTGttOD+J4COhYFOBwu6wUERAUqISFLau/qxH750B0bHJLBB"
            + "Ad4qqP4H9Gsuvuv6qDqyMe5NJoIo4PUM0wTnMNCRW1PzW2iQ33BLLV1if+2pV1pCd+41GI838+X3UyiFR6APDuRVJQIhDaKhMIkn"
            + "3r3tnsvfjwu+H+Lu55q178JLVVoIp1z8w5vCytA74+6+FCCRP6NdHJjHtMvvoQCd9WPKDgXoZX35Aj4g0PWXAkAyGgzSePqD2+65"
            + "7F0++ED5b8CI0VGSY2MiXXPxD66XQX0jpV0oShIBER5q0LOrAwDdHbVt818Jur5SKhEyCoWMoNL2u7fd89L350FX+ZX7vlY4Ovrl"
            + "YGzsinTNn/7g5SIIPiFkdSTtTWdr1sL9X/SHcpP6QEAvesT9dGUe3TlB9/h1uiSlAEEyagYq7Y0DyRu3/fSyr+iMByXHnO91anf0"
            + "9D+9fVUVzQ+LILoMIKRJK3u6VuRvRLBBlOlPKRh9QXcHtl+g21vYX9ANJ/sLejZoBZCQQV0CgFK9r6eq+/btP79iGy5YH+LuDY7b"
            + "4ce8L9ZqSwCANRfddakSwbukDJ8tZAiVtEGqR0RCZXHC7iXNB3rx8v8X0MkmZYKkEJEQsgpQAqXSnxKlN/3uZ5fdCgBzab4+Fvhm"
            + "83qJ9e8BNggFAGsu/uFFlNJVCuoiIeSRQoQAKRAlIEq9FLNkYMUR6rF5/fq+lrUxX14bOnDQ3aoFTQcg86cjsjcniWKQSp8A5B0Q"
            + "6vPb7rnsjqzB+txFb3D+TWjZsV+vlnNrAIAT1n1vRIroXCI6H1BridSxICwVoAZ7M3c/Qc/q7B/oJaVloBe69wXsg87LiEBoAWIP"
            + "gK0kxCYQftBT6sdP/uJl46bdArSeH/8PeEbkd88D9qcAAAAASUVORK5CYII=";
}
