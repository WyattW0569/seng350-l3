module Demo
{
    sequence<string> StringSeq;
    sequence<byte> ImgPayload;

    struct BusMsg {
        int busId;
        string textTag;
        string timestamp;
        ImgPayload image;
    };

    interface Callback{
        void notifyCallback(string msg);
    }
    interface ChatManager{

        void subscribe(Callback* callback);

        StringSeq getState();

        void sendMessage(BusMsg msg);
    }

}